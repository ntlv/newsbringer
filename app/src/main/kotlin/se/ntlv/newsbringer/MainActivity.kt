package se.ntlv.newsbringer

import android.app.LoaderManager
import android.content.CursorLoader
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import se.ntlv.newsbringer.NewsThreadAdapter.ViewHolder
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.network.DataPullPushService


public class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {

    override fun onOffsetChanged(p0: AppBarLayout?, p1: Int) {
        mSwipeView.isEnabled = p1 == 0
    }

    override fun onPause() {
        super.onPause();
        mAppBar.removeOnOffsetChangedListener(this);
    }

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

    val mAdapter: NewsThreadAdapter by lazy(LazyThreadSafetyMode.NONE) {
        NewsThreadAdapter(R.layout.list_item_news_thread)
    }

    private val mAppBar: AppBarLayout by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.appbar) as AppBarLayout
    }

    private val mSwipeView: SwipeRefreshLayout by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.swipe_view) as SwipeRefreshLayout
    }

    private val mRecyclerView: RecyclerView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.recycler_view) as RecyclerView
    }

    private val mProgress by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.progress_view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(BuildConfig.DEBUG.not()) {
            Fabric.with(this, Crashlytics());
        }

        setContentView(R.layout.activity_linear_vertical_content)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        title = getString(R.string.frontpage)

        mSwipeView.setOnRefreshListener { refresh(isCallFromSwipeView = true) }
        mSwipeView.setColorSchemeResources(R.color.primary_color)

        mRecyclerView.setHasFixedSize(true);

        mRecyclerView.layoutManager = LinearLayoutManager(this);

        //TODO set adapter

        mRecyclerView.adapter = mAdapter
        mAdapter.clickListener = { openComments(it) }
        mAdapter.longClickListener = { toggleStarredState(it) }

        mProgress.visibility = View.VISIBLE
        loaderManager.initLoader<Cursor>(R.id.loader_frontpage, null, this)
    }

    override fun onResume() {
        super.onResume()
        mAppBar.addOnOffsetChangedListener(this);
    }

    private fun toggleStarredState(holder: NewsThreadAdapter.ViewHolder?) {
        val id = holder?.id
        if (id != null) {
            DataPullPushService.startActionToggleStarred(this, id)
        }
    }

    private fun openComments(holder: NewsThreadAdapter.ViewHolder?) = holder?.openAsLink()

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

