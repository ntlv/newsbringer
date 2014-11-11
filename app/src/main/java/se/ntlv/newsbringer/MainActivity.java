package se.ntlv.newsbringer;


import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;

import se.ntlv.newsbringer.database.NewsContentProvider;
import se.ntlv.newsbringer.database.PostTable;
import se.ntlv.newsbringer.network.DataPullPushService;


public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
	public static int STOP_ANIMATING = 1;
	private static String[] PROJECTION = new String[]{
			PostTable.COLUMN_SCORE,
			PostTable.COLUMN_TIMESTAMP,
			PostTable.COLUMN_BY,
			PostTable.COLUMN_TEXT,
			PostTable.COLUMN_TITLE,
			PostTable.COLUMN_URL,
			PostTable.COLUMN_ORDINAL,
			PostTable.COLUMN_ID
	};
	MenuItem mRefreshItem;
	boolean mStopAnimation = false;
	private NewsThreadListAdapter mAdapter;

	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mAdapter = new NewsThreadListAdapter(this, R.layout.list_item, null, 0);

		ListView listView = (ListView) findViewById(R.id.list_view);
		listView.setAdapter(mAdapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				openLink(view);
			}
		});
		getLoaderManager().initLoader(0, null, this);
	}

	void openLink(View view) {
		NewsThreadListAdapter.ViewHolder viewHolder = (NewsThreadListAdapter.ViewHolder) view.getTag();
		String uri = viewHolder.link;
		if (uri != null) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
		}
	}

	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		mRefreshItem = menu.findItem(R.id.refresh);
		if (mRefreshItem != null && mRefreshItem.getActionView() != null) {
			mRefreshItem.getActionView().setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mRefreshItem != null) {
						menu.performIdentifierAction(mRefreshItem.getItemId(), 0);
					}
				}
			});
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@Nullable MenuItem item) {
		if (item == null) {
			return false;
		}
		switch (item.getItemId()) {
			case R.id.refresh:
				return handleRefresh(item);
			case R.id.action_settings:
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
		return new CursorLoader(this, NewsContentProvider.CONTENT_URI, PROJECTION, null, null, PostTable.COLUMN_ORDINAL + " DESC");
	}

	public void onLoadFinished(Loader<Cursor> cursorLoader, @Nullable Cursor cursor) {
		mAdapter.swapCursor(cursor);
	}

	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		mAdapter.swapCursor(null);
	}

	void toggleAnimation(boolean shouldStart, @Nullable final MenuItem menuItem) {
		if (shouldStart) {
			mStopAnimation = false;

			Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotation);
			rotation.setRepeatCount(Animation.INFINITE);
			rotation.setAnimationListener(new AbstractAnimationListener() {
				@Override
				public void onAnimationRepeat(Animation animation) {
					if (mStopAnimation) {
						if (menuItem != null) {
							menuItem.getActionView().clearAnimation();
						}
					}
				}
			});

			mRefreshItem.getActionView().startAnimation(rotation);
		} else {
			mStopAnimation = true;
		}
	}

	boolean handleRefresh(final MenuItem menuItem) {
		Messenger messenger = new Messenger(new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				if (msg.what == STOP_ANIMATING) {
					toggleAnimation(false, menuItem);
					return true;
				}
				return false;
			}
		}));

		DataPullPushService.startActionFetchThreads(this, messenger);
		toggleAnimation(true, menuItem);
		return true;
	}

	static class AbstractAnimationListener implements Animation.AnimationListener {
		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}
}
