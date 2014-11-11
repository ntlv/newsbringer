package se.ntlv.newsbringer.network;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import se.ntlv.newsbringer.MainActivity;
import se.ntlv.newsbringer.database.NewsContentProvider;

public class DataPullPushService extends IntentService {

	public static String PARAM_MESSENGER = "messenger";
	public static String TAG = DataPullPushService.class.getSimpleName();
	private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError volleyError) {
			Log.e(TAG, volleyError.toString());
		}
	};
	public static String ACTION_FETCH_THREADS = TAG + "action_fetch_threads";
	public String URI_SUFFIX = ".json";
	public String BASE_URI = "https://hacker-news.firebaseio.com/v0";
	public String ITEM_URI = BASE_URI + "/item";
	public String TOP_HUNDRED_URI = BASE_URI + "/topstories" + URI_SUFFIX;
	RequestQueue mQueue;
	private Response.Listener<NewsThread> mNewsThreadResponseHandler = new Response.Listener<NewsThread>() {
		@Override
		public void onResponse(NewsThread newsThread) {
			getContentResolver().insert(NewsContentProvider.CONTENT_URI, newsThread.getAsContentValues());
		}
	};

	/**
	 * Creates an IntentService.  Invoked by your subclass's constructor.
	 *
	 * @param name Used to name the worker thread, important only for debugging.
	 */
	public DataPullPushService(String name) {
		super(name);
	}

	public DataPullPushService(){
		super(TAG);
	}
	/**
	 * Starts this service to fetch threads from Hacker News.
	 *
	 * @see IntentService
	 */
	public static void startActionFetchThreads(Context context, @Nullable Messenger messenger) {
		Intent intent = new Intent(context, DataPullPushService.class);
		intent.putExtra(PARAM_MESSENGER, messenger);
		intent.setAction(ACTION_FETCH_THREADS);
		context.startService(intent);
	}

	public void onDestroy() {
		Log.i(TAG, "Destroying service");
		super.onDestroy();
	}

	public void onHandleIntent(@Nullable Intent intent) {
		if (intent == null) {
			return;
		}
		String action = intent.getAction();
		Messenger messenger = intent.getParcelableExtra(PARAM_MESSENGER);

		mQueue = Volley.newRequestQueue(this);

		if (ACTION_FETCH_THREADS.equals(action)) {
			mQueue.add(makeTopHundredRequest(messenger));
		} else {
			Log.e(DataPullPushService.TAG, "invalid action");
		}
	}

	JsonArrayRequest makeTopHundredRequest(@Nullable final Messenger messenger) {

		Response.Listener<JSONArray> topHundredListener = new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray jsonArray) {
				handleTopHundredResponse(jsonArray, messenger);
			}
		};
		return new JsonArrayRequest(TOP_HUNDRED_URI, topHundredListener, mErrorListener);
	}

	void handleTopHundredResponse(JSONArray array, @Nullable Messenger messenger) {
		getContentResolver().delete(NewsContentProvider.CONTENT_URI, null, null);
		int length = array.length();
		String url;

		for (int i = 0; i < length - 1; i++) {
			try {
				url = ITEM_URI + "/" + array.get(i) + URI_SUFFIX;
				boolean isLastRequest = i == (length - 1);
				mQueue.add(makeNewsThreadRequest(url, messenger, isLastRequest));
			} catch (JSONException e) {
				Log.e(TAG, e.getCause().toString());
			}
		}
	}

	GsonRequest<NewsThread> makeNewsThreadRequest(String url,
												  @Nullable final Messenger messenger,
												  boolean shouldMessage) {

		Response.Listener<NewsThread> listener;
		if (shouldMessage) {
			listener = new Response.Listener<NewsThread>() {
				@Override
				public void onResponse(NewsThread newsThread) {
					mNewsThreadResponseHandler.onResponse(newsThread);
					final Message message = Message.obtain();
					message.what = MainActivity.STOP_ANIMATING;
					if (messenger != null) {
						try {
							messenger.send(message);
						} catch (RemoteException e) {
							Log.e(TAG, e.getCause().toString());
						}
					}
				}
			};
		} else {
			listener = mNewsThreadResponseHandler;
		}
		return new GsonRequest<>(url, NewsThread.class, null, listener, mErrorListener);
	}
}

