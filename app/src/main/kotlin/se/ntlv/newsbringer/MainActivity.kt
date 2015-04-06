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
import android.util.Log
import se.ntlv.newsbringer.NewsThreadListAdapter.ViewHolder
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


public class MainActivity : Activity(), AbstractCursorLoaderCallbacks {

    override val mAdapter: NewsThreadListAdapter by Delegates.lazy {
        NewsThreadListAdapter(this, R.layout.list_item, null, 0)
    }

    private val mSwipeView: SwipeRefreshLayout by Delegates.lazy {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    var mHiding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super< Activity>.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics());
        val intent = getIntent()

        if (intent == null) {
            Log.d("Main activity got an empty intent", "intent = null")
            return
        }
        Log.d("Main activity got an intent", "intent = $intent")

        setContentView(R.layout.activity_swipe_refresh_list_view_layout)
        setTitle(getString(R.string.frontpage))


        mSwipeView.setOnRefreshListener { refresh(isCallFromSwipeView = true) }
        mSwipeView.setColorSchemeResources(android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light)


        val listView = findViewById(R.id.list_view) as ListView
        listView.setAdapter(mAdapter)
        listView.setOnItemClickListener { adapterView, view, i, l -> openComments(view) }
        listView.setOnItemLongClickListener { adapterView, view, i, l -> openLink(view); true }
        getLoaderManager().initLoader<Cursor>(0, null, this)
    }

    private fun openLink(view: View) = (view.getTag() as? NewsThreadListAdapter.ViewHolder)?.link?.openAsLink()

    private fun openComments(view: View) = (view.getTag() as? NewsThreadListAdapter.ViewHolder)?.openAsLink()

    [suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")]
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
            else -> super< Activity>.onOptionsItemSelected(item)
        }
    }

    fun refresh(isCallFromSwipeView: Boolean = false) {
        if (isCallFromSwipeView.not()) {
            mSwipeView.setRefreshing(true)
        }
        DataPullPushService.startActionFetchThreads(this)
    }

    fun hideRefresh() {
        if (mHiding.not()) {
            mHiding = true
            mSwipeView.postDelayed({ mHiding = false; mSwipeView.setRefreshing(false) }, 1000)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        mSwipeView.setRefreshing(true)
        val projection = array(
                PostTable.COLUMN_SCORE,
                PostTable.COLUMN_TIMESTAMP,
                PostTable.COLUMN_BY,
                PostTable.COLUMN_TEXT,
                PostTable.COLUMN_TITLE,
                PostTable.COLUMN_URL,
                PostTable.COLUMN_ORDINAL,
                PostTable.COLUMN_ID,
                PostTable.COLUMN_CHILDREN
        )
        return CursorLoader(this, NewsContentProvider.CONTENT_URI_POSTS, projection, null, null, PostTable.COLUMN_ORDINAL + " DESC")
    }

    override fun getOnLoadFinishedCallback(): ((Cursor?) -> Unit)? = { if (it != null && it.getCount() > 0 ) hideRefresh() }
    override fun getOnLoaderResetCallback(): ((t: Loader<Cursor>?) -> Unit)? = { hideRefresh() }

}

