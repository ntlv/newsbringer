package se.ntlv.newsbringer

import android.app.Activity
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView

import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.network.DataPullPushService
import android.content.Intent
import android.net.Uri
import android.view.View
import kotlin.properties.Delegates
import android.support.v4.widget.SwipeRefreshLayout
import se.ntlv.newsbringer.NewsThreadListAdapter.ViewHolder


public class MainActivity : Activity(), AbstractCursorLoaderCallbacks {

    override val mAdapter: NewsThreadListAdapter by Delegates.lazy {
        NewsThreadListAdapter(this, R.layout.list_item, null, 0)
    }

    private val mSwipeView: SwipeRefreshLayout by Delegates.lazy {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super< Activity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_swipe_refresh_list_view_layout)

        mSwipeView.setOnRefreshListener { refresh(isCallFromSwipeView = true) }
        mSwipeView.setColorScheme(android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light)


        val listView = findViewById(R.id.list_view) as ListView
        listView.setAdapter(mAdapter)
        listView.setOnItemClickListener {(adapterView, view, i, l) -> openComments(view) }
        listView.setOnItemLongClickListener {(adapterView, view, i, l) -> openLink(view); true }
        getLoaderManager().initLoader<Cursor>(0, null, this)
    }

    private fun openLink(view: View) = (view.getTag() as? NewsThreadListAdapter.ViewHolder)?.link?.openAsLink()

    private fun openComments(view: View) = (view.getTag() as? NewsThreadListAdapter.ViewHolder)?.openAsLink()

    fun String?.openAsLink() = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(this)))
    fun ViewHolder.openAsLink() = startActivity(NewsThreadActivity.getIntent(this@MainActivity, this.metadata))

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item?.getItemId()) {
            R.id.refresh -> {
                refresh(); true
            }
            R.id.action_settings -> true
            else -> super< Activity>.onOptionsItemSelected(item)
        }
    }

    fun refresh(isCallFromSwipeView: Boolean = false) {
        if (!isCallFromSwipeView) {
            mSwipeView.setRefreshing(true)
        }
        DataPullPushService.startActionFetchThreads(this)
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        mSwipeView.setRefreshing(true)
        return CursorLoader(this, NewsContentProvider.CONTENT_URI_POSTS, PROJECTION, null, null, PostTable.COLUMN_ORDINAL + " DESC")
    }

    override fun getOnLoadFinishedCallback(): ((Cursor?) -> Unit)? = { mSwipeView.setRefreshing(false) }
    override fun getOnLoaderResetCallback(): ((t: Loader<Cursor>?) -> Unit)? = { mSwipeView.setRefreshing(false) }

    class object {
        private val PROJECTION = array(
                PostTable.COLUMN_SCORE,
                PostTable.COLUMN_TIMESTAMP,
                PostTable.COLUMN_BY,
                PostTable.COLUMN_TEXT,
                PostTable.COLUMN_TITLE,
                PostTable.COLUMN_URL,
                PostTable.COLUMN_ORDINAL,
                PostTable.COLUMN_ID
        )
    }
}

