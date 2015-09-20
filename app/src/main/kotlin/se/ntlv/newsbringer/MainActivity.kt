package se.ntlv.newsbringer

import android.app.Activity
import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import se.ntlv.newsbringer.NewsThreadListAdapter.ViewHolder
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.network.DataPullPushService


public class MainActivity : Activity(), LoaderManager.LoaderCallbacks<Cursor> {
    override fun onLoaderReset(loader: Loader<Cursor>?) {
        mAdapter.swapCursor(null)
        mSwipeView.isRefreshing = false
        mProgress.visibility = View.INVISIBLE
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) {
        mAdapter.swapCursor(data)
        if (data == null) {
            return
        }
        if (data.count > 0) {
            mSwipeView.isRefreshing = false
            mAdapter.swapCursor(data)
            mProgress.visibility = View.INVISIBLE
        } else {
            mProgress.visibility = View.VISIBLE
        }
    }

    val mAdapter: NewsThreadListAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NewsThreadListAdapter(this, R.layout.list_item, null, 0)
    }

    private val mSwipeView: SwipeRefreshLayout by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    private val mListView: ListView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.list_view) as ListView
    }

    private val mProgress by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.progress_view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics());

        setContentView(R.layout.activity_swipe_refresh_list_view_layout)
        title = getString(R.string.frontpage)

        mSwipeView.setOnRefreshListener { refresh(isCallFromSwipeView = true) }
        mSwipeView.setColorSchemeResources(android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light)


        mListView.adapter = mAdapter
        mListView.setOnItemClickListener { adapterView, view, i, l -> openComments(view) }
        mListView.setOnItemLongClickListener { adapterView, view, i, l -> toggleStarredState(view); true }

        mProgress.visibility = View.VISIBLE
        loaderManager.initLoader<Cursor>(R.id.loader_frontpage, null, this)
    }

    private fun toggleStarredState(view: View) {
        val tag = (view.tag as? NewsThreadListAdapter.ViewHolder)?.id
        if (tag != null) {
            DataPullPushService.startActionToggleStarred(this, tag)
        }
    }

    private fun openLink(view: View) = (view.tag as? NewsThreadListAdapter.ViewHolder)?.link?.openAsLink()

    private fun openComments(view: View) = (view.tag as? NewsThreadListAdapter.ViewHolder)?.openAsLink()

    //    [suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")]
    fun String?.openAsLink() = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(this)))

    fun ViewHolder.openAsLink() = startActivity(CommentsActivity.getIntent(this@MainActivity, this.id ?: -1))

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.refresh -> {
                refresh(); true
            }
            R.id.toggle_show_starred_only -> {
                toggleDisplayStarredOnly(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun refresh(isCallFromSwipeView: Boolean = false) {
        if (isCallFromSwipeView.not()) {
            mSwipeView.isRefreshing = true
        }
        DataPullPushService.startActionFetchThreads(this)
    }

    private var mShowOnlyStarred = false

    private fun toggleDisplayStarredOnly() {
        mShowOnlyStarred = mShowOnlyStarred.not()
        loaderManager.restartLoader(R.id.loader_frontpage, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        mSwipeView.isRefreshing = true
        val selection = if (mShowOnlyStarred) PostTable.STARRED_SELECTION else null
        val selectionArgs = if (mShowOnlyStarred) arrayOf(PostTable.STARRED_SELECTION_ARGS) else null

        return CursorLoader(this, NewsContentProvider.CONTENT_URI_POSTS,
                PostTable.getFrontPageProjection(), selection, selectionArgs, PostTable.getOrdinalSortingString())
    }
}

