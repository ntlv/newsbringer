package se.ntlv.newsbringer.network

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log

import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest

import org.json.JSONArray

import se.ntlv.newsbringer.database.NewsContentProvider
import com.android.volley.RequestQueue
import kotlin.properties.Delegates
import com.android.volley.toolbox.Volley
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.database.CommentsTable
import android.net.Uri
import android.content.ContentResolver
import com.crashlytics.android.Crashlytics

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class DataPullPushService : IntentService(DataPullPushService.TAG) {

    val resolver: ContentResolver   by Delegates.lazy { getContentResolver() }
    val mQueue: RequestQueue        by Delegates.lazy { Volley.newRequestQueue(this) }

    private val mErrorListener = object : Response.ErrorListener {
        override fun onErrorResponse(error: VolleyError) {
            Crashlytics.log("failed to fetch: ${error.getCause()}")
            Log.e(TAG, error.toString())
        }
    }

    private val mJSONArrayListener = object : Response.Listener<JSONArray> {
        override fun onResponse(jsonArray: JSONArray) {
            jsonArray.forEach { mQueue.add(createNewsThreadRequest("${ITEM_URI}${it}${URI_SUFFIX}")) }
        }
    }

    fun JSONArray.forEach(f: (t: Any) -> Unit) {
        val length = this.length()
        (0..length - 1).forEach { i -> f(get(i)) }
    }


    private val mNewsThreadResponseListener = object : Response.Listener<NewsThread> {
        override fun onResponse(response: NewsThread) {
            resolver.insert(NewsContentProvider.CONTENT_URI_POSTS, response.contentValue)
        }
    }

    fun Intent?.doAction() {
        if (this == null) {
            return
        }
        when (this.getAction()) {
            ACTION_FETCH_THREADS -> handleFetchThreads()
            ACTION_FETCH_COMMENTS -> fetchComments(this.getLongExtra(EXTRA_NEWSTHREAD_ID, -1L), this.getBooleanExtra(EXTRA_DISALLOW_FETCH, false))
            ACTION_FETCH_CHILD_COMMENTS -> fetchChildComments(this.getLongExtra(EXTRA_PARENT_COMMENT_ID, -1L), this.getLongExtra(EXTRA_NEWSTHREAD_ID, -1L))
            else -> Log.d(TAG, "Attempted to start $TAG with illegal argument")
        }
    }

    override fun onHandleIntent(intent: Intent?) = intent?.doAction()

    private fun createTopHundredRequest(): JsonArrayRequest {
        return JsonArrayRequest(TOP_HUNDRED_URI, mJSONArrayListener, mErrorListener)
    }

    private fun createNewsThreadRequest(url: String): GsonRequest<NewsThread> {
        return createNewsThreadRequest(mNewsThreadResponseListener, mErrorListener, url)
    }

    private fun createThreadRefreshRequest(id: Long, onCompletion: () -> Unit): GsonRequest<NewsThread> {
        val onResponse: Response.Listener<NewsThread> = Response.Listener {
            resolver.insert(NewsContentProvider.CONTENT_URI_POSTS, it.contentValue)
            onCompletion.invoke()
        }
        return createNewsThreadRequest(onResponse, mErrorListener, "$ITEM_URI$id$URI_SUFFIX")
    }

    private fun createNewsThreadRequest(listener: Response.Listener<NewsThread>,
                                        errorListener: Response.ErrorListener,
                                        url: String): GsonRequest<NewsThread> {
        return GsonRequest(url, javaClass<NewsThread>(), null, listener, errorListener)
    }

    fun createCommentRequest(ordinal: Int,
                             id: Long,
                             ancestorCount: Int = 0,
                             ancestorOrdinal: Double = 0.0,
                             threadParent: Long): GsonRequest<Comment> {
        val url = "${ITEM_URI}${id}${URI_SUFFIX}"
        val listener = object : Response.Listener<Comment> {
            override fun onResponse(response: Comment) {
                val reified = response.getAsContentValues(ordinal, ancestorCount, ancestorOrdinal, threadParent)
                resolver.insert(NewsContentProvider.CONTENT_URI_COMMENTS, reified)
                if (ancestorCount in 0..4 ) {
                    fetchChildComments(response.id, threadParent)
                }
            }
        }
        return GsonRequest(url, javaClass<Comment>(), null, listener, mErrorListener)
    }

    class object {

        public val TAG: String = javaClass<DataPullPushService>().getSimpleName()

        val EXTRA_NEWSTHREAD_ID: String = "${TAG}extra_newsthread_id"
        val EXTRA_DISALLOW_FETCH: String = "${TAG}extra_disallow_fetch_skip"
        val EXTRA_PARENT_COMMENT_ID: String = "${TAG}extra_parent_comment_id"

        val ACTION_FETCH_CHILD_COMMENTS: String = "${TAG}action_fetch_child_comments"
        val ACTION_FETCH_COMMENTS: String = "${TAG}_action_fetch_comments"

        public val ACTION_FETCH_THREADS: String = "${TAG}_action_fetch_threads"

        public var URI_SUFFIX: String = ".json"
        public var BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
        public var ITEM_URI: String = "$BASE_URI/item/"
        public var TOP_HUNDRED_URI: String = "$BASE_URI/topstories$URI_SUFFIX"

        /**
         * Starts this service to fetch threads from Hacker News.
         *
         * @see IntentService
         */
        public fun startActionFetchThreads(context: Context) {
            val intent = Intent(context, javaClass<DataPullPushService>())
            intent.setAction(ACTION_FETCH_THREADS)
            context.startService(intent)
        }

        public fun startActionFetchComments(context: Context, id: Long, disallowFetchSkip: Boolean) {

            Log.d(TAG, "Starting action fetch comments for $id")
            val intent = Intent(context, javaClass<DataPullPushService>())
            intent.setAction(ACTION_FETCH_COMMENTS)
            intent.putExtra(EXTRA_NEWSTHREAD_ID, id)
            intent.putExtra(EXTRA_DISALLOW_FETCH, disallowFetchSkip)
            context.startService(intent)
        }

        fun startActionFetchChildComments(context: Context,
                                          id: Long,
                                          newsThread: Long) {
            Log.d(TAG, "Starting action fetch comments for $id")
            val intent = Intent(context, javaClass<DataPullPushService>())
            intent.setAction(ACTION_FETCH_CHILD_COMMENTS)
            intent.putExtra(EXTRA_PARENT_COMMENT_ID, id)
            intent.putExtra(EXTRA_NEWSTHREAD_ID, newsThread)
            context.startService(intent)
        }
    }

    fun refreshComments(id: Long) {
        val projection = array(
                PostTable.COLUMN_ID,
                PostTable.COLUMN_CHILDREN
        )
        val commentsSelection = "${PostTable.COLUMN_ID}=$id"
        val result = resolver.query(NewsContentProvider.CONTENT_URI_POSTS, projection, commentsSelection, null, null)

        if (result.moveToFirst()) {
            val kids = result.getString(result.getColumnIndexOrThrow(PostTable.COLUMN_CHILDREN))
            if (kids.isEmpty().not()) {
                kids.split(',')
                        .map { it.trim().toLong() }
                        .withIndex()
                        .forEach { mQueue.add(createCommentRequest(it.index, it.value, threadParent = id)) }
            }
        }
        result.close()
    }

    fun fetchComments(newsthreadId: Long, disallowFetchSkip: Boolean) {
        if (newsthreadId == -1L) {
            throw IllegalArgumentException("Thread id can't be -1")
        }
        val uri = NewsContentProvider.CONTENT_URI_COMMENTS
        val selection = "${CommentsTable.COLUMN_PARENT}=?"
        val selectionArgs = array(newsthreadId.toString())
        val commentsExists = hasComments(uri, selection, selectionArgs)
        when {
            commentsExists && !disallowFetchSkip -> return
            commentsExists && disallowFetchSkip ->
                resolver.delete(uri, selection, selectionArgs) //user wants fresh comments, fetch again
        }
        mQueue.add(createThreadRefreshRequest(newsthreadId, { refreshComments(newsthreadId) }))
    }

    fun fetchChildComments(parentComment: Long, parentThread: Long) {
        if (parentComment == -1L || parentThread == -1L) {
            throw IllegalArgumentException("Thread id can't be -1")
        }
        val uri = NewsContentProvider.CONTENT_URI_COMMENTS
        val selection = "${CommentsTable.COLUMN_ID}=?"
        val selectionArgs = array(parentComment.toString())
        val projection = array(
                CommentsTable.COLUMN_ID,
                CommentsTable.COLUMN_ANCESTOR_COUNT,
                CommentsTable.COLUMN_ORDINAL,
                CommentsTable.COLUMN_KIDS
        )
        val parentRow = resolver.query(uri, projection, selection, selectionArgs, null)
        if (parentRow.moveToFirst()) {
            val kids = parentRow.getString(parentRow.getColumnIndexOrThrow(CommentsTable.COLUMN_KIDS))
            val ancestorCount = parentRow.getInt(parentRow.getColumnIndexOrThrow(CommentsTable.COLUMN_ANCESTOR_COUNT))
            val ancestorOrdinal = parentRow.getDouble(parentRow.getColumnIndexOrThrow(CommentsTable.COLUMN_ORDINAL))

            if (kids.isEmpty().not()) {
                kids.split(',')
                        .map { it.trim().toLong() }
                        .withIndex()
                        .forEach {
                            mQueue.add(createCommentRequest(it.index + 1, it.value, ancestorCount + 1, ancestorOrdinal, parentThread))
                        }
            }
        }
        parentRow.close()
    }

    private fun hasComments(uri: Uri,
                            selection: String,
                            selArgs: Array<String>): Boolean {

        val existingCommentsQuery = resolver.query(uri, array(CommentsTable.COLUMN_PARENT), selection, selArgs, null)
        val commentsExists = existingCommentsQuery.getCount() > 0
        existingCommentsQuery.close()
        return commentsExists
    }

    fun handleFetchThreads() {
        resolver.delete(NewsContentProvider.CONTENT_URI_POSTS, null, null)
        mQueue.add(createTopHundredRequest())
    }
}

