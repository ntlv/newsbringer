package se.ntlv.newsbringer.comments

import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import rx.Observable
import rx.Subscriber
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.customviews.applyAppBarLayoutDependency
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.network.RowItem
import se.ntlv.newsbringer.newsthreads.completeAllAndVerify
import java.nio.BufferOverflowException

interface CommentsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun updateContent(data: AdapterModelCollection<RowItem>)

    fun observeRefreshEvents(): Observable<Any>
}

class UiBinder(private val mActivity: CommentsActivity,
               private val mManager: LinearLayoutManager,
               private val mAdapter: CommentsAdapterWithHeader) : AppBarLayout.OnOffsetChangedListener, AnkoLogger, CommentsViewBinder {

    private val mRefreshListeners: MutableList<Subscriber<in Any>> = mutableListOf()
    private val mAppBar: AppBarLayout = mActivity.find<AppBarLayout>(R.id.appbar)
    private val mSwipeView: SwipeRefreshLayout = mActivity.find<SwipeRefreshLayout>(R.id.swipe_view)

    lateinit var refreshButtonManager: RefreshButtonAnimator

    init {
        mSwipeView.setOnRefreshListener { mRefreshListeners.forEach { it.onNext(0) } }
        mSwipeView.setColorSchemeResources(R.color.accent_color)

        val recyclerView = mActivity.find<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = mManager
        recyclerView.adapter = mAdapter

        val fab = mActivity.find<FloatingActionButton>(R.id.fab)
        fab.applyAppBarLayoutDependency()

        fab.onClick { navigateByScrolling(Direction.DOWN) }
        fab.onLongClick { navigateByScrolling(Direction.UP) }
    }

    private enum class Direction {
        UP {
            override fun start(manager: LinearLayoutManager) = manager.findFirstVisibleItemPosition()
            override val mover: (Int) -> Int = Int::dec
        },
        DOWN {
            override fun start(manager: LinearLayoutManager) = manager.findLastVisibleItemPosition()
            override val mover: (Int) -> Int = Int::inc
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
        mSwipeView.isEnabled = 0 == verticalOffset
    }

    override fun indicateDataLoading(isLoading: Boolean) {
        if (mSwipeView.isRefreshing != isLoading) {
            mSwipeView.isRefreshing = isLoading
        }
        refreshButtonManager.indicateLoading(isLoading)
    }

    override fun observeRefreshEvents(): Observable<Any> =
            Observable.create<Any> { mRefreshListeners.add(it) }.onBackpressureBuffer(10, { throw BufferOverflowException() })

    override fun updateContent(data: AdapterModelCollection<RowItem>) {
        val maybeHeader = data[0]
        if (maybeHeader is RowItem.NewsThreadUiData) {
            mActivity.title = maybeHeader.title.starify(maybeHeader.isStarred)
        }
        mAdapter.data = data
    }

    fun start() = mAppBar.addOnOffsetChangedListener(this)

    fun stop() = mAppBar.removeOnOffsetChangedListener(this)

    fun destroy() = completeAllAndVerify(mRefreshListeners)
}
