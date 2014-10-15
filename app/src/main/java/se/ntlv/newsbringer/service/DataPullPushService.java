package se.ntlv.newsbringer.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import se.ntlv.newsbringer.database.DatabaseHelper;
import se.ntlv.newsbringer.database.NewsContentProvider;
import se.ntlv.newsbringer.database.PostTable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class DataPullPushService extends IntentService {

	public static final String TAG = "Fetcher";
	private Response.Listener<JSONArray> mListener = new Response.Listener<JSONArray>() {

		@Override
		public void onResponse(JSONArray response) {
			final ContentResolver contentResolver = getContentResolver();
			contentResolver.delete(NewsContentProvider.CONTENT_URI, null, null); //empty the table
			final ContentValues cv = new ContentValues();
			for (int i = 0; i < response.length(); i++) {
				try {
					storePost(cv, contentResolver, i, response.getInt(i));
					cv.clear();
				} catch (JSONException ignored) {
					//carry on
				}
			}
		}
	};

	private void storePost(ContentValues cv, ContentResolver contentResolver, int i, int postId) {
		final long timeStamp = System.currentTimeMillis();
		cv.put(PostTable.COLUMN_ID, postId);
		cv.put(PostTable.COLUMN_ORDINAL, i);
		cv.put(PostTable.COLUMN_TIMESTAMP, timeStamp);
		contentResolver.insert(NewsContentProvider.CONTENT_URI, cv);
	}

	private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Log.e(TAG, error.toString());
		}
	};

	private enum Actions{
		ACTION_FETCH_THREADS
	}

	public static String sTopHundred = "https://hacker-news.firebaseio.com/v0/topstories.json";


    /**
     * Starts this service to fetch threads from Hacker News.
     *
     * @see IntentService
     */
    public static void startActionFetchThreads(Context context) {
        Intent intent = new Intent(context, DataPullPushService.class);
        intent.setAction(Actions.ACTION_FETCH_THREADS.name());
        context.startService(intent);
    }

    public DataPullPushService() {
        super("DataPullPushService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
			try {
				switch (Actions.valueOf(intent.getAction())) {
					case ACTION_FETCH_THREADS:
						handleActionFetchThreads();
						break;
				}

			} catch (IllegalArgumentException ignored) {
				//attempted to start invalid action, ignore for now
			}
		}
    }

    /**
     * Handle action FETCH_THREADS in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetchThreads() {
		final RequestQueue volley = getVolley();

		volley.add(new JsonArrayRequest(sTopHundred, mListener, mErrorListener));

    }

	private RequestQueue getVolley() {
		return VolleySingleton.getInstance(getApplicationContext()).getRequestQueue();
	}
}
