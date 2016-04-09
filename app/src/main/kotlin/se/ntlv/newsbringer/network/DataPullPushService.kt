package se.ntlv.newsbringer.network

import android.app.IntentService
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.info
import org.jetbrains.anko.verbose
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.database.NewsContentProvider.Companion.CONTENT_URI_COMMENTS
import se.ntlv.newsbringer.database.NewsContentProvider.Companion.CONTENT_URI_POSTS
import java.io.File
import java.io.IOException
import java.io.Reader

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
class DataPullPushService : IntentService(DataPullPushService.TAG) {

    companion object {

        //TODO abstract this
        val MAX_THREAD_IDX = 500

        private val TAG: String = DataPullPushService::class.java.simpleName

        private val EXTRA_NEWSTHREAD_ID: String = "${TAG}extra_newsthread_id"
        private val EXTRA_ALLOW_FETCH_SKIP: String = "${TAG}extra_disallow_fetch_skip"
        private val EXTRA_CURRENT_MAX: String = "${TAG}extra_current_max"
        private val EXTRA_DO_FULL_WIPE: String = "${TAG}extra_do_full_wipe"

        private val ACTION_FETCH_COMMENTS: String = "${TAG}_action_fetch_comments"
        private val ACTION_FETCH_THREADS: String = "${TAG}_action_fetch_threads"

        private val ACTION_TOGGLE_STARRED: String = "${TAG}action_toggle_starred"

        var URI_SUFFIX: String = ".json"
        var BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
        var ITEM_URI: String = "$BASE_URI/item/"
        var TOP_FIFTY_URI: String = "$BASE_URI/topstories$URI_SUFFIX"

        /**
         * Starts this service to fetch threads from Hacker News.
         *
         * @see IntentService
         */
        fun startActionFetchThreads(context: Context, currentMax: Int = 0, doFullWipe: Boolean) {
            val intent = Intent(context, DataPullPushService::class.java)
            intent.action = ACTION_FETCH_THREADS
            intent.putExtra(EXTRA_CURRENT_MAX, currentMax)
            intent.putExtra(EXTRA_DO_FULL_WIPE, doFullWipe)
            if (context.startService(intent) == null) {
                throw IllegalStateException("Unable to start data service")
            }
        }

        fun startActionFetchComments(context: Context, id: Long, allowFetchSkip: Boolean) {
            val intent = Intent(context, DataPullPushService::class.java)
                    .setAction(ACTION_FETCH_COMMENTS)
                    .putExtras(bundleOf(
                            Pair(EXTRA_NEWSTHREAD_ID, id),
                            Pair(EXTRA_ALLOW_FETCH_SKIP, allowFetchSkip))
                    )
            if (context.startService(intent) == null) {
                throw IllegalStateException("Unable to start data service")
            }
        }

        fun startActionToggleStarred(context: Context, id: Long): Boolean {
            val intent = Intent(context, DataPullPushService::class.java)
                    .setAction(ACTION_TOGGLE_STARRED)
                    .putExtra(EXTRA_NEWSTHREAD_ID, id)
            return context.startService(intent) != null
        }
    }

    private val resolver: ContentResolver by lazy { contentResolver }

    private val gson: Gson by lazy { Gson() }

    private val okHttp: OkHttpClient by lazy {
        val httpCacheDirectory: File = File(cacheDir, "responses");
        val cacheSize: Long = 10 * 1024 * 1024; // 10 MiB
        OkHttpClient.Builder()
                .cache(Cache(httpCacheDirectory, cacheSize))
                .build()
    }


    private fun Intent?.doAction() {
        if (this == null) {
            return
        }
        when (this.action) {
            ACTION_FETCH_THREADS -> handleFetchThreads(
                    this.getIntExtra(EXTRA_CURRENT_MAX, -1),
                    this.getBooleanExtra(EXTRA_DO_FULL_WIPE, false)
            )
            ACTION_FETCH_COMMENTS -> handleFetchComments(
                    this.getLongExtra(EXTRA_NEWSTHREAD_ID, -1L),
                    this.getBooleanExtra(EXTRA_ALLOW_FETCH_SKIP, true)
            )
            ACTION_TOGGLE_STARRED -> toggleStarred(
                    this.getLongExtra(EXTRA_NEWSTHREAD_ID, -1L))
            else -> throw IllegalArgumentException("Attempted to start $TAG with illegal argument ${this.action}")
        }
    }

    override fun onHandleIntent(intent: Intent?): Unit = intent?.doAction() as Unit

    private fun toggleStarred(id: Long) {
        val projection = arrayOf(PostTable.COLUMN_ID, PostTable.COLUMN_STARRED)
        val starred = resolver.getRowById(CONTENT_URI_POSTS, projection, id, { it.getIntByName(PostTable.COLUMN_STARRED) })

        val newStatus = (starred + 1) % 2
        val cv = ContentValues(1)
        cv.put(PostTable.COLUMN_STARRED, newStatus)

        resolver.updateRowById(CONTENT_URI_POSTS, id, cv)
    }

    fun handleFetchComments(newsthreadId: Long, allowFetchSkip: Boolean) {
        // valid id?
        if (newsthreadId == -1L) {
            throw IllegalArgumentException("Thread id can't be -1")
        }
        // can skip fetching?
        if (checkCanSkipFetch(newsthreadId, allowFetchSkip)) {
            return
        }
        val projection = PostTable.getOrdinalAndStarredProjection()
        val ordinalSelection = "${PostTable.COLUMN_ID}=$newsthreadId"

        val ordinalCursor = resolver.query(CONTENT_URI_POSTS, projection, ordinalSelection)
        ordinalCursor.moveToPositionOrThrow(0)

        val ordinal = ordinalCursor.getIntByName(PostTable.COLUMN_ORDINAL)
        val isStarred = ordinalCursor.getIntByName(PostTable.COLUMN_STARRED)
        ordinalCursor.close()

        val thread = "$ITEM_URI$newsthreadId$URI_SUFFIX".blockingGetRequestToModel<NewsThread>(okHttp, gson)
        resolver.insert(CONTENT_URI_POSTS, thread?.toContentValues(ordinal, isStarred == 1))

        var index = 0
        thread?.kids?.forEach {
            index = getAllChildComments(index, it, 0, newsthreadId)
        }

    }

    private fun checkCanSkipFetch(id: Long, skipsAllowed: Boolean): Boolean {
        val uri = CONTENT_URI_COMMENTS
        val projection = arrayOf(CommentsTable.COLUMN_PARENT)
        val selection = "${CommentsTable.COLUMN_PARENT}=?"
        val selectionArgs = arrayOf(id.toString())

        val existingCommentsQuery = resolver.query(uri, projection, selection, selectionArgs)
        val commentsExists = existingCommentsQuery.count > 0

        existingCommentsQuery.close()
        return when {
            commentsExists && skipsAllowed -> true
            commentsExists && skipsAllowed.not() -> {
                resolver.delete(uri, selection, selectionArgs); false
            }
            else -> false
        }
    }


    inline fun <reified T> String.blockingGetRequestToModel(client: OkHttpClient, gson: Gson): T? {
        var reader: Reader? = null
        var body: ResponseBody? = null
        try {
            val request = Request.Builder().url(this).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful.not()) throw IOException("Unexpected response { $response }")
            body = response.body()
            reader = body.charStream()

            return gson.fromJson<T>(reader, object : TypeToken<T>() {}.type)
        } finally {
            reader?.close()
            body?.close()
        }
    }

    fun handleFetchThreads(currentMax: Int, doFullWipe: Boolean) {
        if ((currentMax !in 0..490) and !doFullWipe) {
            throw IllegalArgumentException("Invalid range of current max")
        }
        if (doFullWipe) {
            val starredProjection = PostTable.getOrdinalAndStarredProjection()
            val sel = PostTable.STARRED_SELECTION
            val starredSelArgs = arrayOf(PostTable.STARRED_SELECTION_ARGS)

            val starredThreads = resolver.query(CONTENT_URI_POSTS, starredProjection, sel, starredSelArgs)
            val starredAndOrdinalIds = starredThreads.toList({
                Pair(it.getLongByName(PostTable.COLUMN_ID), it.getIntByName(PostTable.COLUMN_ORDINAL))
            })
            val starredIds = starredAndOrdinalIds.map { it.first }
            starredThreads.close()

            resolver.delete(CONTENT_URI_POSTS, null, null)

            val ids = TOP_FIFTY_URI.blockingGetRequestToModel<Array<Long>>(okHttp, gson)
            val emptyNewsThreads = ids?.withIndex()
                    ?.filter { it.value !in starredIds }
                    ?.map { NewsThread(it.value).toContentValues(it.index) }

            val starredItems = starredAndOrdinalIds.map {
                val item = "$ITEM_URI${it.first}$URI_SUFFIX".blockingGetRequestToModel<NewsThread>(okHttp, gson)
                val ordinal = it.second
                item?.toContentValues(ordinal, true)
            }

            val compound = starredItems + (emptyNewsThreads ?: emptyList<ContentValues>())

            if (compound.isNotEmpty()) {
                resolver.bulkInsert(CONTENT_URI_POSTS, compound.toTypedArray())
            } else {
                Toast.makeText(baseContext, "Unable to prepare data load", Toast.LENGTH_SHORT).show()
            }
        }

        val proj = PostTable.getFrontPageProjection()
        val sel = "${PostTable.COLUMN_TITLE} is null or ${PostTable.COLUMN_TITLE} = ''"
        val orderBy = " ${PostTable.COLUMN_ORDINAL} ASC "
        val limit = (if (doFullWipe) 10 else currentMax) + 10
        val emptyThreadsCursor = resolver.query(NewsContentProvider.CONTENT_URI_POSTS_W_LIMIT(limit), proj, sel, null, orderBy)
        emptyThreadsCursor.toList {
            Pair(it.getLongByName(PostTable.COLUMN_ID), it.getIntByName(PostTable.COLUMN_ORDINAL))
        }.forEach {
            val item = "$ITEM_URI${it.first}$URI_SUFFIX".blockingGetRequestToModel<NewsThread>(okHttp, gson)
            val cv = item?.toContentValues(it.second)
            cv?.let {
                resolver.insert(CONTENT_URI_POSTS, it)
            }
        }
        emptyThreadsCursor.close()
    }

    /**
     * Hand this function a origin comment and it will insert it into database as well
     * as fetch all of its children and add those to DB as well.
     */
    private fun getAllChildComments(startOrdinal: Int, targetId: Long, ancestorCount: Int, threadParentId: Long): Int {

        val comment = "$ITEM_URI$targetId$URI_SUFFIX".blockingGetRequestToModel<Comment>(okHttp, gson)

        val reified = comment?.toContentValues(startOrdinal, ancestorCount, threadParentId)
        if (reified != null) {
            resolver.insert(CONTENT_URI_COMMENTS, reified)
        }
        var index = startOrdinal

        comment?.kids?.forEach {
            index.inc()
            index = getAllChildComments(index, it, ancestorCount + 1, threadParentId)
        }
        return index + 1
    }
}

