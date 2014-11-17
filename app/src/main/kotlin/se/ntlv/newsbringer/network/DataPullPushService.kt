package se.ntlv.newsbringer.network

import android.app.IntentService
import android.content.ContentValues
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
import java.util.ArrayList
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.database.CommentsTable

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class DataPullPushService : IntentService(DataPullPushService.TAG) {

    val mQueue: RequestQueue by Delegates.lazy {
        Volley.newRequestQueue(this)
    }

    val mTemporaryResponseStorage: ArrayList<ContentValues> by Delegates.lazy {
        ArrayList<ContentValues>(100)
    }

    private val mErrorListener = object : Response.ErrorListener {
        override fun onErrorResponse(error: VolleyError) {
            Log.e(TAG, error.toString())
        }

    }
    private val mJSONArrayListener = object : Response.Listener<JSONArray> {
        override fun onResponse(jsonArray: JSONArray) {
            jsonArray.forEach { mQueue.add(getNewsThreadRequest("${ITEM_URI}/${it}${URI_SUFFIX}")) }
        }
    }

    fun JSONArray.forEach(f: (t: Any) -> Unit) {
        val length = this.length()
        (0..length - 1).forEach { i -> f(get(i)) }
    }


    private val mNewsThreadResponseListener = object : Response.Listener<NewsThread> {
        override fun onResponse(response: NewsThread) {
            mTemporaryResponseStorage.add(response.getAsContentValues())
            if (mTemporaryResponseStorage.size == 100) {
                getContentResolver().delete(NewsContentProvider.CONTENT_URI_POSTS, null, null) //empty the table
                getContentResolver().bulkInsert(NewsContentProvider.CONTENT_URI_POSTS, mTemporaryResponseStorage.copyToArray())
            }
        }
    }

    fun Intent?.doAction() {
        when (this?.getAction()) {
            ACTION_FETCH_THREADS -> mQueue.add(getTopHundredRequest())
            ACTION_FETCH_COMMENTS -> fetchComments(this?.getLongExtra(EXTRA_NEWSTHREAD_ID, -1L))
            else -> Log.d(TAG, "Attempted to start $TAG with illegal argument")
        }
    }

    override fun onHandleIntent(intent: Intent?) = intent?.doAction()

    private fun getTopHundredRequest(): JsonArrayRequest {
        return JsonArrayRequest(TOP_HUNDRED_URI, mJSONArrayListener, mErrorListener)
    }

    private fun getNewsThreadRequest(url: String): GsonRequest<NewsThread> {
        return getNewsThreadRequest(mNewsThreadResponseListener, mErrorListener, url)
    }

    private fun getNewsThreadRequest(listener: Response.Listener<NewsThread>,
                                     errorListener: Response.ErrorListener,
                                     url: String): GsonRequest<NewsThread> {
        return GsonRequest(url, javaClass<NewsThread>(), null, listener, errorListener)
    }

    fun makeCommentRequest(ordinal: Int, id: Long): GsonRequest<Comment> {
        val url = "${ITEM_URI}/${id}${URI_SUFFIX}"
        val listener = object : Response.Listener<Comment> {
            override fun onResponse(response: Comment) {
                getContentResolver().insert(NewsContentProvider.CONTENT_URI_COMMENTS, response.getAsContentValues(ordinal))
            }
        }
        return GsonRequest(url, javaClass<Comment>(), null, listener, mErrorListener)
    }

    class object {

        public val TAG: String = javaClass<DataPullPushService>().getSimpleName()

        val EXTRA_NEWSTHREAD_ID: String = "${TAG}extra_newsthread_id"

        public var URI_SUFFIX: String = ".json"
        public var BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
        public var ITEM_URI: String = "$BASE_URI/item"
        public var TOP_HUNDRED_URI: String = "$BASE_URI/topstories$URI_SUFFIX"
        public val ACTION_FETCH_THREADS: String = "${TAG}_action_fetch_threads"
        val ACTION_FETCH_COMMENTS: String = "${TAG}_action_fetch_comments"

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

        public fun startActionFetchComments(context: Context, id: Long?) {
            if (id == null) {
                return
            }
            Log.d(TAG, "Starting action fetch comments for $id")
            val intent = Intent(context, javaClass<DataPullPushService>())
            intent.setAction(ACTION_FETCH_COMMENTS)
            intent.putExtra(EXTRA_NEWSTHREAD_ID, id)
            context.startService(intent)
        }
    }

    fun isAlreadyFetched(id : Long) : Boolean {
        val uri = NewsContentProvider.CONTENT_URI_COMMENTS
        val projection = array(CommentsTable.COLUMN_ID, CommentsTable.COLUMN_PARENT)
        val selection = "${CommentsTable.COLUMN_PARENT}=$id"
        val exists = (getContentResolver().query(uri, projection, selection, null, null).getCount() < 1).not()
        Log.d(TAG, "id: $id, exists: $exists")
        return exists
    }

    fun fetchComments(newsthreadId: Long?) {
        if (newsthreadId == null || newsthreadId == -1L || isAlreadyFetched(newsthreadId)) {
            return
        }
        val projection = array(PostTable.COLUMN_ID, PostTable.COLUMN_CHILDREN)
        val selection = "${PostTable.COLUMN_ID}=${newsthreadId}"
        val result = getContentResolver().query(NewsContentProvider.CONTENT_URI_POSTS, projection, selection, null, null)
        Log.d(TAG, "Query done, result size is ${result.getCount()}" )
        if (result.moveToFirst()) {
            //we have some results, it is safe to do things with the cursor
            Log.d(TAG, "Got results, mapping on cursor $result")
            result.getString(result.getColumnIndexOrThrow(PostTable.COLUMN_CHILDREN))
                    .split(',')
                    .map { it.toLong() }
                    .withIndices()
                    .forEach { mQueue.add(makeCommentRequest(it.first, it.second)) }

        }
        result.close()
    }
}

