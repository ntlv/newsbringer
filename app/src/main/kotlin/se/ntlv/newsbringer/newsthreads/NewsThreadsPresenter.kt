package se.ntlv.newsbringer.newsthreads

import rx.Subscription
import rx.android.schedulers.AndroidSchedulers.mainThread
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.DataLoadingFacilitator


class NewsThreadsPresenter(val viewBinder: NewsThreadsViewBinder,
                           val navigator: Navigator,
                           val interactor: NewsThreadsInteractor) : DataLoadingFacilitator {

    var subscription: Subscription? = null
    var starredOnly = false
    var filter = ""

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        subscription?.unsubscribe()
        subscription = interactor.loadData(starredOnly, filter).observeOn(mainThread()).subscribe {
            viewBinder.presentData(it)
            viewBinder.indicateDataLoading(false)
        }
    }

    fun refreshData() {
        viewBinder.indicateDataLoading(true)
        interactor.downloadData()
    }

    fun toggleShowOnlyStarred() {
        viewBinder.toggleDynamicLoading()
        starredOnly = !starredOnly
        onViewReady()
    }

    fun onItemClick(itemId: Long): Unit = navigator.navigateToItemComments(itemId)

    fun onItemLongClick(itemId: Long, starredStatus : Int) = interactor.toggleItemStarredState(itemId, starredStatus)

    override fun onMoreDataNeeded(currentMaxItem: Int) {
        if (interactor.downloadMoreData(currentMaxItem)) {
            viewBinder.showStatusMessage(R.string.status_loading_more_data)
            viewBinder.indicateDataLoading(true)
        }
    }

    fun filter(newText: String?) {
        filter = newText ?: ""
        onViewReady()
    }

    fun destroy() = subscription?.unsubscribe()
}

