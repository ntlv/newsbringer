package se.ntlv.newsbringer.network

import android.app.IntentService
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.database.NewsContentProvider.Companion.CONTENT_URI_COMMENTS
import se.ntlv.newsbringer.database.NewsContentProvider.Companion.CONTENT_URI_POSTS
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
class DataPullPushService : IntentService(DataPullPushService.TAG), AnkoLogger {

    companion object {

        //TODO abstract this
        val MAX_THREAD_IDX = 500

        private val TAG: String = DataPullPushService::class.java.simpleName

        private val EXTRA_NEWSTHREAD_ID: String = "${TAG}extra_newsthread_id"
        private val EXTRA_ALLOW_FETCH_SKIP: String = "${TAG}extra_disallow_fetch_skip"
        private val EXTRA_FETCH_RANGE_START: String = "${TAG}extra_fetch_range_start"
        private val EXTRA_FETCH_RANGE_END: String = "${TAG}extra_fetch_range_end"
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
        fun startActionFetchThreads(context: Context, start: Int = 0, end: Int = 9, doFullWipe: Boolean) {
            val intent = Intent(context, DataPullPushService::class.java)
            intent.action = ACTION_FETCH_THREADS
            intent.putExtra(EXTRA_FETCH_RANGE_START, start)
            intent.putExtra(EXTRA_FETCH_RANGE_END, end)
            intent.putExtra(EXTRA_DO_FULL_WIPE, doFullWipe)
            if (context.startService(intent) == null) {
                throw IllegalStateException("Unable to start data service")
            }
        }

        fun startActionFetchComments(context: Context, id: Long, allowFetchSkip: Boolean) {

            Log.d(TAG, "Starting action fetch comments for $id")
            val intent = Intent(context, DataPullPushService::class.java)
            intent.action = ACTION_FETCH_COMMENTS
            intent.putExtra(EXTRA_NEWSTHREAD_ID, id)
            intent.putExtra(EXTRA_ALLOW_FETCH_SKIP, allowFetchSkip)
            if (context.startService(intent) == null) {
                throw IllegalStateException("Unable to start data service")
            }
        }

        fun startActionToggleStarred(context: Context, id: Long): Boolean {
            val intent = Intent(context, DataPullPushService::class.java)
            intent.action = ACTION_TOGGLE_STARRED
            intent.putExtra(EXTRA_NEWSTHREAD_ID, id)
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
        verbose("Service handling ${this.action}")
        when (this.action) {
            ACTION_FETCH_THREADS -> handleFetchThreads(
                    this.getIntExtra(EXTRA_FETCH_RANGE_START, -1),
                    this.getIntExtra(EXTRA_FETCH_RANGE_END, -1),
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
        val selection = "${PostTable.COLUMN_ID}=?"
        val selectionArgs = arrayOf("$id")
        val result = resolver.query(CONTENT_URI_POSTS, projection, selection, selectionArgs)
        if (!result.moveToFirst()) {
            return
        }
        val currentStarredStatus = result.getIntByName(PostTable.COLUMN_STARRED).equals(1)
        val newStatus = if (currentStarredStatus) 0 else 1
        val cv = ContentValues(1)
        cv.put(PostTable.COLUMN_STARRED, newStatus)
        resolver.update(CONTENT_URI_POSTS, cv, selection, selectionArgs)
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
        val projection = PostTable.getIdAndOrdinalProjection()
        val ordinalSelection = "${PostTable.COLUMN_ID}=$newsthreadId"

        val ordinalCursor = resolver.query(CONTENT_URI_POSTS, projection, ordinalSelection)
        if (ordinalCursor.moveToFirst().not()) {
            throw IllegalArgumentException("NewsThread does not exist in DB")
        }
        val ordinal = ordinalCursor.getIntByName(PostTable.COLUMN_ORDINAL)
        ordinalCursor.close()

        val thread = "$ITEM_URI$newsthreadId$URI_SUFFIX".blockingGetRequestToModel<NewsThread>(okHttp, gson)
        resolver.insert(CONTENT_URI_POSTS, thread?.toContentValues(ordinal))

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
        return Request.Builder().url(this).get().build().blockingCallToModel(client, gson)
    }

    inline fun <reified T> Request.blockingCallToModel(client: OkHttpClient, gson: Gson): T? {
        try {
            val body = client.newCall(this).execute().body()
            val bytes = BufferedReader(InputStreamReader(body.byteStream()))
            return gson.fromJson<T>(bytes, object : TypeToken<T>() {}.type)
        } catch(ex: IOException) {
            return null
        } catch(ex: IllegalStateException) {
            return null
        } catch(ex: JsonParseException) {
            return null
        } catch(ex: JsonSyntaxException) {
            return null
        }
    }

    fun handleFetchThreads(start: Int, end: Int, doFullWipe : Boolean) {
        if (start == -1 || end == -1) {
            throw IllegalArgumentException("Invalid fetch range")
        }
        val sel : String
        val selArgs : Array<out String>
        if (doFullWipe) {
            sel = PostTable.STARRED_SELECTION
            selArgs = arrayOf(PostTable.UNSTARRED_SELECTION_ARGS)
        } else {
            sel = PostTable.STARRED_SELECTION_W_RANGE
            selArgs = arrayOf(PostTable.UNSTARRED_SELECTION_ARGS, start.toString(), end.toString())
        }
        resolver.delete(CONTENT_URI_POSTS, sel, selArgs)

        val starredProjection = arrayOf(PostTable.COLUMN_ID, PostTable.COLUMN_STARRED)
        val starredSel = PostTable.STARRED_SELECTION
        val starredSelArgs = arrayOf(PostTable.STARRED_SELECTION_ARGS)

        val starredThreads = resolver.query(CONTENT_URI_POSTS, starredProjection, starredSel, starredSelArgs)
        val starredIds = starredThreads.toList({it.getLongByName(PostTable.COLUMN_ID)})
        starredThreads.close()

        val ids = TOP_FIFTY_URI.blockingGetRequestToModel<Array<Long>>(okHttp, gson)
        val batch = ids
                ?.mapIndexed { idx, itemId -> Pair(idx, itemId) }
                ?.filter { it.first in start..end && it.second !in starredIds}
                ?.map {
                    val item = "$ITEM_URI${it.second}$URI_SUFFIX".blockingGetRequestToModel<NewsThread>(okHttp, gson)
                    item?.toContentValues(it.first)
                }
                ?.filterNotNull()
                ?.toTypedArray()
        if (batch != null) {
            resolver.bulkInsert(CONTENT_URI_POSTS, batch)
        }
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

