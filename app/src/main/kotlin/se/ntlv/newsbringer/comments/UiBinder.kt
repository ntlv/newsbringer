package se.ntlv.newsbringer.comments

import android.support.design.widget.AppBarLayout
import android.support.design.widget.AppBarLayout.OnOffsetChangedListener
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import rx.Emitter
import rx.Observable
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.application.createTrackedEmitterWithAutoRemove
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.network.CommentUiData
import se.ntlv.newsbringer.network.NewsThreadUiData
import se.ntlv.newsbringer.network.RowItem

interface CommentsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun updateContent(data: AdapterModelCollection<RowItem>)

    fun observeRefreshEvents(): Observable<Any>
}

class UiBinder(private val mActivity: CommentsActivity,
               private val mManager: LinearLayoutManager,
               private val mAdapter: CommentsAdapterWithHeader) : OnOffsetChangedListener, CommentsViewBinder {

    private val mRefreshListeners: MutableList<Emitter<Any>> = mutableListOf()
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
        recyclerView.addOnScrollListener(FabManager(fab))

        val threadStartPredicate: (RowItem?) -> Boolean = { it is CommentUiData && it.ancestorCount == 0 }
        val findAndScroll: (IntProgression) -> Unit = {
            val maybeTarget = mAdapter.indexOfThreadStarterCommentIn(it, threadStartPredicate)
            if (maybeTarget != null) {
                Snackbar.make(recyclerView, "Scrolling to $maybeTarget", Snackbar.LENGTH_SHORT).show()
                mManager.scrollToPositionWithOffset(maybeTarget, 0)
            } else {
                Snackbar.make(recyclerView, "Can't scroll further", Snackbar.LENGTH_SHORT).show()
            }

        }

        fab.onClick {
            val searchSpace = mManager.findLastVisibleItemPosition() until mManager.itemCount
            findAndScroll(searchSpace)
        }
        fab.onLongClick {
            val lastVisible = mManager.findFirstVisibleItemPosition()
            val searchSpace = 0.until(lastVisible).reversed()
            findAndScroll(searchSpace)
            true
        }
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

    override fun observeRefreshEvents(): Observable<Any> = createTrackedEmitterWithAutoRemove(mRefreshListeners)

    override fun updateContent(data: AdapterModelCollection<RowItem>) {
        mAdapter.data = data
        if (data.isNotEmpty()) {
            val maybeHeader = data[0]
            if (maybeHeader is NewsThreadUiData) {
                mActivity.title = maybeHeader.title.starify(maybeHeader.isStarred)
            }
        }
    }

    fun start() = mAppBar.addOnOffsetChangedListener(this)

    fun stop() = mAppBar.removeOnOffsetChangedListener(this)

    fun destroy() {
        mRefreshListeners.forEach { it.onCompleted() }
        mRefreshListeners.clear()
    }
}
