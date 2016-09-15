package se.ntlv.newsbringer.newsthreads

import android.content.Context
import android.view.View
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData
import se.ntlv.newsbringer.thisShouldNeverHappen

interface NewsThreadsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun presentData(data: Data<NewsThreadUiData>)

    fun showStatusMessage(@StringRes messageResource: Int)

    fun observerPresentationProgress(): Observable<Pair<Int, Float>>

    fun observeRefreshEvents(): Observable<Any>
}

class UiBinder(activity: NewsThreadsActivity,
               manager: RecyclerView.LayoutManager,
               private val adapter: NewsThreadAdapter) : AppBarLayout.OnOffsetChangedListener, AnkoLogger, NewsThreadsViewBinder {

    private val mRefreshListeners: MutableList<Subscriber<in Any>> = mutableListOf()

    private val mAppBar = activity.find<AppBarLayout>(R.id.appbar)
    private val mSwipeView = activity.find<SwipeRefreshLayout>(R.id.swipe_view)
    private val mRecyclerView = activity.find<RecyclerView>(R.id.recycler_view)
    private val mToaster: Context = activity

    lateinit var refreshButtonManager: RefreshButtonAnimator

    init {
        activity.find<FloatingActionButton>(R.id.fab).visibility = View.GONE
        mSwipeView.setOnRefreshListener { mRefreshListeners.forEach { it.onNext(0) } }
        mSwipeView.setColorSchemeResources(R.color.accent_color)
        mRecyclerView.layoutManager = manager
        mRecyclerView.adapter = adapter
    }

    override fun presentData(data: Data<NewsThreadUiData>) = adapter.updateContent(data)

    override fun showStatusMessage(messageResource: Int) = mToaster.toast(messageResource)

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mSwipeView.isEnabled = 0.equals(verticalOffset)
    }

    override fun observerPresentationProgress() = adapter.observe()

    override fun observeRefreshEvents(): Observable<Any> =
            Observable.create<Any> { mRefreshListeners.add(it) }.onBackpressureLatest()

    fun start() {
        mAppBar.addOnOffsetChangedListener(this)
    }


    fun stop() {
        mAppBar.removeOnOffsetChangedListener(this)
    }

    fun destroy() = completeAllAndVerify(mRefreshListeners)

    override fun indicateDataLoading(isLoading: Boolean) {
        if (mSwipeView.isRefreshing != isLoading) {
            mSwipeView.isRefreshing = isLoading
        }
        refreshButtonManager.indicateLoading(isLoading)
    }
}

fun <T> completeAllAndVerify(subscribers: List<Subscriber<in T>>) {
    subscribers.forEach { it.onCompleted() }
    if (subscribers.any { !it.isUnsubscribed }) {
        thisShouldNeverHappen()
    }
}
