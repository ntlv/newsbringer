package se.ntlv.newsbringer.network

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.info
import se.ntlv.newsbringer.application.GlobalDependency
import se.ntlv.newsbringer.network.IoService.Action.*
import se.ntlv.newsbringer.thisShouldNeverHappen
import java.io.IOException
import kotlin.LazyThreadSafetyMode.NONE


class IoService : MultithreadedIntentService(), AnkoLogger {
    companion object : AnkoLogger {
        fun requestFetchComment(context: Context, id: Long, ancestorCount: Int, baseOrdinal: String, newsThreadId: Long) =
                startService(context, FETCH_COMMENT,
                        ARG_ID to id,
                        ARG_ANCESTOR_COUNT to ancestorCount,
                        ARG_BASE_ORDINAL to baseOrdinal,
                        ARG_THREAD_ID to newsThreadId)

        fun requestFetchPostAndComments(context: Context, id: Long) =
                startService(context, FETCH_COMMENTS, ARG_ID to id)

        fun requestFetchThreads(context: Context, range: IntRange): Boolean {
            val normalizedRange = when {
                range.last < ORDINAL_MAX_BOUND -> range
                range.first < ORDINAL_MAX_BOUND && range.last >= ORDINAL_MAX_BOUND -> range.first..499
                range.first > ORDINAL_MAX_BOUND -> IntRange.EMPTY
                else -> thisShouldNeverHappen()
            }
            normalizedRange.forEach { startService(context, FETCH_THREAD, ARG_ORDINAL to it) }
            return true
        }

        fun requestFullWipe(context: Context) = startService(context, FULL_WIPE)

        fun requestToggleStarred(context: Context, id: Long, currentStarredStatus: Int) =
                startService(context, TOGGLE_STARRED, ARG_ID to id, ARG_IS_STARRED to currentStarredStatus)

        fun requestPrepareHeaderAndCommentsFor(context: Context, newsThreadId: Long) =
                startService(context, PREPARE_CONTENT, ARG_ID to newsThreadId)

        private fun startService(context: Context, action: Action, vararg args: Pair<String, Any>) {
            info("Starting service for $action(${args.joinToString()})")
            val i = Intent(context, IoService::class.java).setAction(action.name).putExtras(bundleOf(*args))
            checkNotNull(context.startService(i))
        }

        private val ARG_ID = "id"
        private val ARG_THREAD_ID = "thread_id"
        private val ARG_ANCESTOR_COUNT = "ancestor_count"
        private val ARG_BASE_ORDINAL = "base_ordinal"
        private val ARG_ORDINAL = "ordinal"
        private val ARG_IS_STARRED = "is_starred"
        private val ORDINAL_MAX_BOUND = 500
    }

    enum class Action { FETCH_COMMENTS, FETCH_COMMENT, TOGGLE_STARRED, FETCH_THREAD, FULL_WIPE, PREPARE_CONTENT }

    private val URI_SUFFIX: String = ".json"
    private val BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
    private val ITEM_URI: String = "$BASE_URI/item/"
    private val TOP_FIVE_HUNDRED: String = "$BASE_URI/topstories$URI_SUFFIX"

    val http by lazy(NONE) { GlobalDependency.httpClient }
    val database by lazy(NONE) { GlobalDependency.database }
    val gson by lazy(NONE) { GlobalDependency.gson }

    override val onBeginJob: (Intent) -> Unit = {
        val args = it.extras
        when (valueOf(it.action)) {
            FETCH_COMMENT -> handleFetchComment(args)
            FETCH_COMMENTS -> handleFetchComments(args)
            FETCH_THREAD -> handleFetchThread(args)
            TOGGLE_STARRED -> handleToggleStarred(args)
            FULL_WIPE -> handleFullWipe()
            PREPARE_CONTENT -> handlePrepareContent(args)
        }
    }

    private fun handlePrepareContent(args: Bundle) {
        val newsThreadId = args[ARG_ID] as Long

        val childIds : List<Long>?
        val newsItem = database.getPostByIdSync(newsThreadId)
        if (newsItem == null) {
            val fetchedItem =  getNewsThread(newsThreadId, 0, 0)
            childIds = fetchedItem.kids?.asList()
            database.insertNewsThreads(fetchedItem)
        } else {
            childIds = newsItem.children.split(',').filter(String::isNotEmpty).map(String::toLong)
        }

        if (!database.hasCommentsForPostIdSync(newsThreadId)) {
            if (childIds == null || childIds.isEmpty()) {
                val fakeComment = Comment()
                fakeComment.text = "No comments"
                fakeComment.parent = newsThreadId
                fakeComment.time = System.currentTimeMillis()
                val cv = fakeComment.toContentValues("a", 0, newsThreadId)
                database.insertComment(cv)
            } else {
                var commentOrdinal = "a"
                childIds.forEach {
                    requestFetchComment(this, it, 0, commentOrdinal, newsThreadId)
                    commentOrdinal = commentOrdinal.nextOrdinal()
                }
            }
        }
    }

    private fun handleFetchComment(args: Bundle) {
        val id = args[ARG_ID] as Long
        val ancestorCount = args[ARG_ANCESTOR_COUNT] as Int
        val baseOrdinal = args[ARG_BASE_ORDINAL] as String
        val newsThreadId = args[ARG_THREAD_ID] as Long

        val url = "$ITEM_URI$id$URI_SUFFIX"

        val comment = http.get(url, Comment::class.java, gson)

        val reified = comment.toContentValues(baseOrdinal, ancestorCount, newsThreadId)
        database.insertComment(reified)

        var childOrdinal = baseOrdinal + 'a'
        val childAncestorCount = ancestorCount.inc()
        comment.kids.forEach {
            requestFetchComment(this, it, childAncestorCount, childOrdinal, newsThreadId)
            childOrdinal = childOrdinal.nextOrdinal()
        }
    }

    private fun handleFetchComments(extras: Bundle) {
        val newsThreadId: Long = extras[ARG_ID] as? Long ?: thisShouldNeverHappen("Missing id")
        val post = database.getPostByIdSync(newsThreadId)

        val thread = getNewsThread(newsThreadId, post?.ordinal ?: ORDINAL_MAX_BOUND, post?.isStarred ?: 0)
        database.insertNewsThreads(thread)


        var commentOrdinal = "a"
        thread.kids?.forEach {
            requestFetchComment(this, it, 0, commentOrdinal, newsThreadId)
            commentOrdinal = commentOrdinal.nextOrdinal()
        }
    }

    private fun handleToggleStarred(args: Bundle) {
        val id = args[ARG_ID] as Long
        val currentStatus = args[ARG_IS_STARRED] as Int

        val newStatus = currentStatus.plus(1).mod(2)
        database.setStarred(id, newStatus)
    }

    private fun String.nextOrdinal(): String {
        val char = last()
        return when (char) {
            in ('a'..'y') -> "${this.subSequence(0, length - 1)}${char.inc()}"
            'z' -> this + 'a'
            else -> thisShouldNeverHappen("Can't increment character $char from base $this")
        }
    }


    private fun handleFullWipe() {

        val starredBeforeDelete = database.allStarredPosts()
        val starredIds = starredBeforeDelete.map { it.id }

        database.deleteFrontPage()

        val ids = http.get(TOP_FIVE_HUNDRED, Array<Long>::class.java, gson)
        val emptyNewsThreads = ids.filter { it !in starredIds }.mapIndexed { ordinal, id -> NewsThread(id, ordinal) }

        val starredItems = starredBeforeDelete.map { getNewsThread(it.id, it.ordinal, 1) }
        val insertThis = (starredItems + emptyNewsThreads).toTypedArray()

        database.insertNewsThreads(*insertThis)

        handleFetchThread(bundleOf(ARG_ORDINAL to 0))
    }

    private fun handleFetchThread(args: Bundle) {
        val ordinal = args[ARG_ORDINAL] as Int

        val id = database.getIdForOrdinalSync(ordinal) ?: http.get(TOP_FIVE_HUNDRED, Array<Long>::class.java, gson)[ordinal]
        val item = getNewsThread(id, ordinal)
        database.insertNewsThreads(item)
    }

    private fun getNewsThread(id: Long, ordinal: Int, isStarred: Int = 0): NewsThread {
        val item = http.get("$ITEM_URI$id$URI_SUFFIX", NewsThread::class.java, gson)
        item.ordinal = ordinal
        item.starred = isStarred
        return item
    }


    private fun <T> OkHttpClient.get(url: String, cls: Class<T>, deserializer: Gson): T {

        try {
            val request = Request.Builder().url(url).get().build()
            newCall(request).execute().use {
                if (!it.isSuccessful) throw IOException("Unexpected response { $it }")

                it.body().use {
                    it.charStream().use {
                        return deserializer.fromJson(it, cls)
                    }
                }

            }
        } catch (ex: Exception) {
            Log.e("IoService", "Http GET failure", ex)
            throw ex
        }
    }

}

