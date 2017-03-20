package se.ntlv.newsbringer.newsthreads

import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.find
import rx.Emitter
import rx.Observable
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.application.createTrackedEmitterWithAutoRemove
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.network.NewsThreadUiData

interface NewsThreadsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun presentData(data: AdapterModelCollection<NewsThreadUiData>)

    fun showStatusMessage(message: String)

    fun observePresentationPosition(): Observable<Int>

    fun observeRefreshEvents(): Observable<Any>
}

class UiBinder(activity: NewsThreadsActivity,
               manager: RecyclerView.LayoutManager,
               private val adapter: NewsThreadAdapter) : AppBarLayout.OnOffsetChangedListener, AnkoLogger, NewsThreadsViewBinder {

    private val mRefreshListeners: MutableList<Emitter<Any>> = mutableListOf()

    private val mAppBar = activity.find<AppBarLayout>(R.id.appbar)
    private val mSwipeView = activity.find<SwipeRefreshLayout>(R.id.swipe_view)
    private val mRecyclerView = activity.find<RecyclerView>(R.id.recycler_view)

    lateinit var refreshButtonManager: RefreshButtonAnimator

    init {
        activity.find<FloatingActionButton>(R.id.fab).visibility = View.GONE
        mSwipeView.setOnRefreshListener { mRefreshListeners.forEach { it.onNext(0) } }
        mSwipeView.setColorSchemeResources(R.color.accent_color)
        mRecyclerView.setHasFixedSize(false)
        mRecyclerView.layoutManager = manager
        manager.isAutoMeasureEnabled = true
        mRecyclerView.adapter = adapter
    }

    override fun presentData(data: AdapterModelCollection<NewsThreadUiData>) {
        adapter.data = data
    }

    override fun showStatusMessage(message: String) = Snackbar.make(mAppBar, message, Snackbar.LENGTH_SHORT).show()

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mSwipeView.isEnabled = 0 == verticalOffset
    }

    override fun observePresentationPosition() = adapter.observePresentationPosition()

    override fun observeRefreshEvents(): Observable<Any> = createTrackedEmitterWithAutoRemove(mRefreshListeners)


    fun start() {
        mAppBar.addOnOffsetChangedListener(this)
    }


    fun stop() {
        mAppBar.removeOnOffsetChangedListener(this)
    }

    fun destroy() {
        mRefreshListeners.forEach { it.onCompleted() }
        mRefreshListeners.clear()
    }

    override fun indicateDataLoading(isLoading: Boolean) {
        if (mSwipeView.isRefreshing != isLoading) {
            mSwipeView.isRefreshing = isLoading
        }
        refreshButtonManager.indicateLoading(isLoading)
    }
}
