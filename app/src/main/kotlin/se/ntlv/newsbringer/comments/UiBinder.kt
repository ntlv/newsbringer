package se.ntlv.newsbringer.comments

import android.app.Activity
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.customviews.applyAppBarLayoutDependency
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.network.RowItem

interface CommentsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun updateContent(data: Data<RowItem>)
}

class UiBinder : AppBarLayout.OnOffsetChangedListener, AnkoLogger, CommentsViewBinder {

    private val mHost: Activity
    private val mAppBar: AppBarLayout
    private val mSwipeView: SwipeRefreshLayout
    private val mManager: LinearLayoutManager
    private val mAdapter: CommentsAdapterWithHeader

    lateinit var refreshButtonManager: RefreshButtonAnimator

    constructor(activity: CommentsActivity,
                refreshListener: () -> Unit,
                manager: LinearLayoutManager,
                adapter: CommentsAdapterWithHeader) {


        mHost = activity
        mAppBar = activity.find<AppBarLayout>(R.id.appbar)
        mManager = manager
        mAdapter = adapter

        mSwipeView = activity.find<SwipeRefreshLayout>(R.id.swipe_view)
        mSwipeView.setOnRefreshListener(refreshListener)
        mSwipeView.setColorSchemeResources(R.color.accent_color)

        val recyclerView = activity.find<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter

        val fab = activity.find<FloatingActionButton>(R.id.fab)
        fab.applyAppBarLayoutDependency()

        fab.onClick { navigateByScrolling(Direction.DOWN) }
        fab.onLongClick { navigateByScrolling(Direction.UP) }
    }

    private enum class Direction {
        UP {
            override fun start(manager: LinearLayoutManager) = manager.findFirstVisibleItemPosition()
            override val mover: (Int) -> Int = { it.dec() }
        },
        DOWN {
            override fun start(manager: LinearLayoutManager) = manager.findLastVisibleItemPosition()
            override val mover: (Int) -> Int = { it.inc() }
        };

        abstract fun start(manager: LinearLayoutManager): Int
        abstract val mover: (Int) -> Int

        val predicate: (RowItem) -> Boolean = { it is RowItem.CommentUiData && it.ancestorCount == 0 }
    }

    private fun navigateByScrolling(direction: Direction): Boolean {
        val target = mAdapter.findInDataSet(direction.start(mManager), direction.predicate, direction.mover)
        mManager.scrollToPositionWithOffset(target, 0)
        return true
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mSwipeView.isEnabled = 0.equals(verticalOffset)
    }

    override fun indicateDataLoading(isLoading: Boolean) {
        if (mSwipeView.isRefreshing != isLoading) {
            mSwipeView.isRefreshing = isLoading
        }
        refreshButtonManager.indicateLoading(isLoading)
    }

    override fun updateContent(data: Data<RowItem>) {
        val maybeHeader = data[0]
        if (maybeHeader is RowItem.NewsThreadUiData) {
            mHost.title = maybeHeader.title.starify(maybeHeader.isStarred)
        }
        mAdapter.updateContent(data)
    }

    fun start() {
        mAppBar.addOnOffsetChangedListener(this)
    }


    fun stop() {
        mAppBar.removeOnOffsetChangedListener(this)
    }
}