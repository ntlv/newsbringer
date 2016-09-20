package se.ntlv.newsbringer.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.info
import se.ntlv.newsbringer.application.YcReaderApplication
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.thisShouldNeverHappen
import java.io.IOException
import java.io.Reader
import java.util.*
import javax.inject.Inject


class IoService : MultithreadedIntentService(), AnkoLogger {
    companion object : AnkoLogger {
        fun requestFetchPostAndComments(context: Context, id: Long) =
                startService(context, Action.FETCH_COMMENTS, ARG_ID to id)

        fun requestFetchThreads(context: Context, range: IntRange): Boolean {
            val normalizedRange = when {
                range.last < 500 -> range
                range.first < 500 && range.last >= 500 -> range.first..499
                range.first > 500 -> IntRange.EMPTY
                else -> thisShouldNeverHappen()
            }
            normalizedRange.forEach { startService(context, Action.FETCH_THREAD, ARG_ORDINAL to it) }
            return true
        }

        fun requestFullWipe(context: Context) = startService(context, Action.FULL_WIPE)

        fun requestToggleStarred(context: Context, id: Long, currentStarredStatus: Int) =
                startService(context, Action.TOGGLE_STARRED, ARG_ID to id, ARG_IS_STARRED to currentStarredStatus)

        private fun startService(context: Context, action: Action, vararg args: Pair<String, Any>) {
            info("Starting service for $action(${args.joinToString()})")
            Intent()
            val i = Intent(context, IoService::class.java).setAction(action.name).putExtras(bundleOf(*args))
            checkNotNull(context.startService(i))
        }

        private val ARG_ID = "id"
        private val ARG_ORDINAL = "ordinal"
        private val ARG_IS_STARRED = "is_starred"
    }

    private enum class Action { FETCH_COMMENTS, TOGGLE_STARRED, FETCH_THREAD, FULL_WIPE }

    override fun onCreate() {
        super.onCreate()
        YcReaderApplication.applicationComponent().inject(this)
    }

    private val URI_SUFFIX: String = ".json"
    private val BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
    private val ITEM_URI: String = "$BASE_URI/item/"
    private val TOP_FIVE_HUNDRED: String = "$BASE_URI/topstories$URI_SUFFIX"

    @Inject lateinit var okHttp: OkHttpClient
    @Inject lateinit var database: Database
    @Inject lateinit var gson: Gson

    override fun onBeginJob(intent: Intent) =
            when (Action.valueOf(intent.action)) {
                Action.FETCH_COMMENTS -> handleFetchComments(intent.extras)
                Action.FETCH_THREAD -> handleFetchThread(intent.extras)
                Action.TOGGLE_STARRED -> handleToggleStarred(intent.extras)
                Action.FULL_WIPE -> handleFullWipe()
                else -> thisShouldNeverHappen()
            }

    private fun handleToggleStarred(args: Bundle) {
        val id = args[ARG_ID] as Long
        val currentStatus = args[ARG_IS_STARRED] as Int

        val newStatus = currentStatus.plus(1).mod(2)
        database.setStarred(id, newStatus)
    }

    private fun handleFetchComments(extras: Bundle) {
        val newsThreadId: Long = extras[ARG_ID] as? Long ?: thisShouldNeverHappen("Missing id")

        val post = database.getPostByIdSync(newsThreadId)

        val thread = getNewsThread(newsThreadId, post.ordinal, post.isStarred)
        database.insertNewsThreads(thread)


        val kids = thread.kids?.map { Work(it, 0) } ?: mutableListOf()
        val work: Stack<Work> = Stack(kids)

        var commentOrdinal = 0

        while (work.isNotEmpty()) {
            val task = work.pop()
            val url = "$ITEM_URI${task.id}$URI_SUFFIX"

            val comment = okHttp.get(url, Comment::class.java, gson)

            val reified = comment.toContentValues(commentOrdinal, task.ancestorCount, newsThreadId)
            database.insertComment(reified)

            //inc one for the inserted comment and then one each for every child comment
            commentOrdinal += 1 + comment.kids.size

            //push all direct children to work stack
            val newAncestorCount = task.ancestorCount + 1
            comment.kids.forEach {
                work.push(Work(it, newAncestorCount))
            }
        }
    }

    private data class Work(val id: Long, val ancestorCount: Int)

    private class Stack<T>(init: List<T>) {
        private val delegate: LinkedList<T> = LinkedList(init)

        fun push(t: T) = delegate.addFirst(t)
        fun pop(): T = delegate.removeFirst()

        fun isNotEmpty() = delegate.isNotEmpty()
    }

    private fun handleFullWipe() {
        val starredBeforeDelete = database.getFrontPage(starredOnly = true)
                .mapToList { RowItem.NewsThreadUiData(it) }
                .toBlocking()
                .first()

        val starredIds = starredBeforeDelete.map { it.id }

        database.deleteFrontPage()

        val ids = okHttp.get(TOP_FIVE_HUNDRED, Array<Long>::class.java, gson)
        val emptyNewsThreads = ids.filter { it !in starredIds }.mapIndexed { ordinal, id -> NewsThread(id, ordinal) }

        val starredItems = starredBeforeDelete.map { getNewsThread(it.id, it.ordinal, 1) }
        val insertThis = (starredItems + emptyNewsThreads).toTypedArray()

        database.insertNewsThreads(*insertThis)

        handleFetchThread(bundleOf(ARG_ORDINAL to 0))
    }

    private fun handleFetchThread(args: Bundle) {
        val ordinal = args[ARG_ORDINAL] as Int

        val id = database.getIdForOrdinalSync(ordinal)

        val item = getNewsThread(id, ordinal)
        database.insertNewsThreads(item)
    }

    private fun getNewsThread(id: Long, ordinal: Int, isStarred: Int = 0): NewsThread {
        val item = okHttp.get("$ITEM_URI$id$URI_SUFFIX", NewsThread::class.java, gson)
        item.ordinal = ordinal
        item.starred = isStarred
        return item
    }


    private fun <T> OkHttpClient.get(url: String, cls: Class<T>, deserializer: Gson): T {
        var reader: Reader? = null
        var body: ResponseBody? = null
        var response: Response? = null
        try {
            val request = Request.Builder().url(url).get().build()
            response = newCall(request).execute()
            if (response.isSuccessful.not()) throw IOException("Unexpected response { $response }")
            body = response.body()
            reader = body.charStream()
            return deserializer.fromJson<T>(reader, cls)
        } finally {
            response?.close()
            reader?.close()
            body?.close()
        }
    }
}

