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
import org.json.JSONException

import java.util.Arrays

import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class DataPullPushService : IntentService(DataPullPushService.TAG) {
    private val mErrorListener = object : Response.ErrorListener {
        override fun onErrorResponse(error: VolleyError) {
            Log.e(TAG, error.toString())
        }
    }
    private val mJSONArrayListener = object : Response.Listener<JSONArray> {
        override fun onResponse(jsonArray: JSONArray) {
            getContentResolver().delete(NewsContentProvider.CONTENT_URI, null, null) //empty the table
            val length = jsonArray.length()
            val url: String
            for (i in 0..length - 1) {
                try {
                    url = ITEM_URI + '/' + jsonArray.get(i) + URI_SUFFIX
                    getVolley().addToRequestQueue<NewsThread>(getNewsThreadRequest(url))
                } catch (ignored: JSONException) {
                }

            }
        }
    }
    private val mListener = object : Response.Listener<NewsThread> {
        override fun onResponse(response: NewsThread) {
            getContentResolver().insert(NewsContentProvider.CONTENT_URI, response.getAsContentValues())
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            try {
                when (Actions.valueOf(intent.getAction())) {
                    DataPullPushService.Actions.ACTION_FETCH_THREADS -> handleActionFetchThreads()
                }

            } catch (ignored: IllegalArgumentException) {
                //attempted to start invalid action, ignore for now
            }
        }
    }

    private fun handleActionFetchThreads() {
        getVolley().addToRequestQueue<JSONArray>(getTopHundredRequest())
    }

    private fun getTopHundredRequest(): JsonArrayRequest {
        return JsonArrayRequest(TOP_HUNDRED_URI, mJSONArrayListener, mErrorListener)
    }

    private fun getNewsThreadRequest(url: String): GsonRequest<NewsThread> {
        return getNewsThreadRequest(null, mListener, mErrorListener, url)
    }

    private fun getNewsThreadRequest(headers: Map<String, String>?, listener: Response.Listener<NewsThread>, errorListener: Response.ErrorListener, url: String): GsonRequest<NewsThread> {

        return GsonRequest(url, javaClass<NewsThread>(), headers, listener, errorListener)
    }

    private fun getVolley(): VolleySingleton {
        return VolleySingleton.getInstance(getApplicationContext())
    }

    private enum class Actions {
        ACTION_FETCH_THREADS
    }

    private inner class NewsThread {
        public var score: Int = 0
        public var time: Long = 0
        public var id: Long = 0
        public var by: String? = null
        public var title: String? = null
        public var kids: LongArray? = null
        public var text: String? = null
        public var type: String? = null
        public var url: String? = null

        public fun getAsContentValues(): ContentValues {
            val cv = ContentValues(9)
            cv.put(PostTable.COLUMN_ID, id)
            cv.put(PostTable.COLUMN_SCORE, score)
            cv.put(PostTable.COLUMN_TIMESTAMP, time)
            cv.put(PostTable.COLUMN_BY, by ?: "Unknown author")
            cv.put(PostTable.COLUMN_TITLE, title ?: "No title")
            cv.put(PostTable.COLUMN_CHILDREN, kids?.joinToString() ?: "no children")
            cv.put(PostTable.COLUMN_TEXT,  text ?: "No text")
            cv.put(PostTable.COLUMN_TYPE, type ?: "Unknown type")
            cv.put(PostTable.COLUMN_URL, url ?: "Unkown URL")

            val unixTime = System.currentTimeMillis().div(1000)
            val hoursSinceSubmission = unixTime.minus(time).div(3600)
            val adjustedScore = (score.minus(1)).toDouble()
            val ordinal = adjustedScore.div(Math.pow((hoursSinceSubmission.plus(2)).toDouble(), 1.8))

            cv.put(PostTable.COLUMN_ORDINAL, ordinal)

            return cv
        }
    }

    class object {

        public val TAG: String = javaClass<DataPullPushService>().getSimpleName()
        public var URI_SUFFIX: String = ".json"
        public var BASE_URI: String = "https://hacker-news.firebaseio.com/v0"
        public var ITEM_URI: String = "$BASE_URI/item"
        public var TOP_HUNDRED_URI: String = "$BASE_URI/topstories$URI_SUFFIX"

        /**
         * Starts this service to fetch threads from Hacker News.
         *
         * @see IntentService
         */
        public fun startActionFetchThreads(context: Context) {
            val intent = Intent(context, javaClass<DataPullPushService>())
            intent.setAction(Actions.ACTION_FETCH_THREADS.name())
            context.startService(intent)
        }
    }
}
