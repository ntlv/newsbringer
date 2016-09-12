package se.ntlv.newsbringer.newsthreads

import android.content.Context
import android.support.annotation.StringRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.find
import org.jetbrains.anko.toast
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.customviews.RefreshButtonAnimator
import se.ntlv.newsbringer.database.TypedCursor
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData

interface NewsThreadsViewBinder {
    fun indicateDataLoading(isLoading: Boolean): Unit

    fun presentData(data: TypedCursor<NewsThreadUiData>?)

    fun showStatusMessage(@StringRes messageResource: Int)

    fun toggleDynamicLoading()
}

class UiBinder(activity: NewsThreadsActivity,
               refreshListener: () -> Unit,
               manager: RecyclerView.LayoutManager,
               private val adapter: NewsThreadAdapter) : AppBarLayout.OnOffsetChangedListener, AnkoLogger, NewsThreadsViewBinder {


    private val mAppBar = activity.find<AppBarLayout>(R.id.appbar)
    private val mSwipeView = activity.find<SwipeRefreshLayout>(R.id.swipe_view)
    private val mRecyclerView = activity.find<RecyclerView>(R.id.recycler_view)
    private val mToaster: Context = activity

    lateinit var refreshButtonManager: RefreshButtonAnimator

    init {
        activity.find<FloatingActionButton>(R.id.fab).visibility = View.GONE
        mSwipeView.setOnRefreshListener(refreshListener)
        mSwipeView.setColorSchemeResources(R.color.accent_color)
        mRecyclerView.layoutManager = manager
        mRecyclerView.adapter = adapter
    }

    override fun presentData(data: TypedCursor<NewsThreadUiData>?) = adapter.updateContent(data)

    override fun showStatusMessage(messageResource: Int) = mToaster.toast(messageResource)

    override fun toggleDynamicLoading() {
        adapter.shouldLoadDataDynamic = !adapter.shouldLoadDataDynamic
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        mSwipeView.isEnabled = 0.equals(verticalOffset)
    }

    fun start() {
        mAppBar.addOnOffsetChangedListener(this)
    }


    fun stop() {
        mAppBar.removeOnOffsetChangedListener(this)
    }

    override fun indicateDataLoading(isLoading: Boolean) {
        if (mSwipeView.isRefreshing != isLoading) {
            mSwipeView.isRefreshing = isLoading
        }
        refreshButtonManager.indicateLoading(isLoading)

    }

}
