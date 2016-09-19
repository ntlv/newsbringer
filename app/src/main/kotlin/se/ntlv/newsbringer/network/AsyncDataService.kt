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
import javax.inject.Inject


class AsyncDataService : MultithreadedIntentService(), AnkoLogger {
    companion object : AnkoLogger {
        fun fetchPostAndCommentsAsync(context: Context, id: Long) =
                startService(context, Action.FETCH_COMMENTS, ARG_ID to id)

        fun requestFetchThread(context: Context, ordinalRange : IntRange) =
                ordinalRange.last <= 500 &&
                        ordinalRange.map {
                            startService(context, Action.FETCH_THREADS, ARG_ORDINAL to it)
                        }.any { it }

        fun requestFullWipe(context: Context) = startService(context, Action.FULL_WIPE)


        fun requestToggleStarred(context: Context, id: Long, currentStarredStatus: Int) =
                startService(context, Action.TOGGLE_STARRED, ARG_ID to id, ARG_IS_STARRED to currentStarredStatus)

        private fun startService(context: Context, action: Action, vararg args: Pair<String, Any>): Boolean {
            info("Starting service for $action(${args.joinToString()})")
            val i = Intent(context, AsyncDataService::class.java).setAction(action.name).putExtras(bundleOf(*args))
            return null != context.startService(i)
        }

        private val ARG_ID = "id"
        private val ARG_ORDINAL = "ordinal"
        private val ARG_IS_STARRED = "is_starred"
    }

    private enum class Action {
        FETCH_COMMENTS, TOGGLE_STARRED, FETCH_THREADS, FULL_WIPE
    }

    private fun String?.asAction(): Action? =
            if (this == null) null
            else Action.valueOf(this)


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

    override fun onBeginJob(intent: Intent?) =
            when (intent?.action?.asAction()) {
                Action.FETCH_COMMENTS -> handleFetchComments(intent!!.extras)
                Action.FETCH_THREADS -> handleFetchThreads(intent!!.extras)
                Action.TOGGLE_STARRED -> handleToggleStarred(intent!!.extras)
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
        val newsThreadId: Long = extras.getLong(ARG_ID, -1L)
        // valid id?
        if (newsThreadId < 1L) {
            throw IllegalArgumentException("Thread id can't be -1")
        }
        val post = database.getPostByIdSync(newsThreadId)

        val thread = okHttp.get("$ITEM_URI$newsThreadId$URI_SUFFIX", NewsThread::class.java, gson)

        thread.ordinal = post.ordinal
        thread.starred = post.isStarred

        database.insertNewsThreads(thread)


        val work: MutableList<Pair<Long, Int>> = thread.kids?.map { it to 0 }?.toMutableList() ?: mutableListOf()

        var commentOrdinal = 0

        while (work.isNotEmpty()) {
            val task = work.removeAt(0)
            val url = "$ITEM_URI${task.first}$URI_SUFFIX"

            val comment = okHttp.get(url, Comment::class.java, gson)

            val reified = comment.toContentValues(commentOrdinal, task.second, newsThreadId)
            database.insertComment(reified)

            //inc one for the inserted comment and then one each for every child comment
            commentOrdinal += 1 + comment.kids.size

            //push all direct children to work stack
            val newAncestorCount = task.second + 1
            comment.kids.forEach {
                work.add(0, it.to(newAncestorCount))
            }
        }
    }

    private fun handleFullWipe() {
        val starredBeforeDelete = database.getFrontPage(starredOnly = true)
                .mapToList { RowItem.NewsThreadUiData(it) }
                .take(1)
                .toBlocking()
                .first()

        val starredIds = starredBeforeDelete.map { it.id }

        database.deleteFrontPage()

        val ids = okHttp.get(TOP_FIVE_HUNDRED, Array<Long>::class.java, gson)
        val emptyNewsThreads = ids.withIndex()
                .filter { it.value !in starredIds }
                .map {
                    val i = NewsThread(it.value)
                    i.starred = 0
                    i.ordinal = it.index
                    i
                }

        val starredItems = starredBeforeDelete.map {
            val itemUrl = "$ITEM_URI${it.id}$URI_SUFFIX"
            val item = okHttp.get(itemUrl, NewsThread::class.java, gson)
            item.starred = 1
            item.ordinal = it.ordinal
            item
        }
        val insertThis = (starredItems + emptyNewsThreads).toTypedArray()

        database.insertNewsThreads(*insertThis)

        requestFetchThread(this, 0..0)
    }

    private fun handleFetchThreads(args: Bundle) {
        val ordinal = args[ARG_ORDINAL] as Int

        val id = database.getIdForOrdinalSync(ordinal)

        val item = okHttp.get("$ITEM_URI$id$URI_SUFFIX", NewsThread::class.java, gson)
        item.ordinal = ordinal
        database.insertNewsThreads(item)
    }

    fun <T> OkHttpClient.get(url: String, cls: Class<T>, deserializer: Gson): T {
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

