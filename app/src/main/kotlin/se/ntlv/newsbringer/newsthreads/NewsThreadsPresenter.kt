package se.ntlv.newsbringer.newsthreads

import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.ObservableData
import se.ntlv.newsbringer.network.NewsThreadUiData


class NewsThreadsPresenter(val viewBinder: NewsThreadsViewBinder,
                           val navigator: Navigator,
                           val interactor: NewsThreadsInteractor) {

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        interactor.loadData({ onDataLoaded(it) })
    }

    fun onDataLoaded(data: ObservableData<NewsThreadUiData>?) {
        viewBinder.data = data
        if (data?.hasContent() ?: false) {
            viewBinder.indicateDataLoading(false)
        }
    }

    fun refreshData(needToIndicateLoading: Boolean) {
        if (needToIndicateLoading) {
            viewBinder.indicateDataLoading(true)
        }
        interactor.refreshData()
    }

    fun toggleShowOnlyStarred() {
        viewBinder.indicateDataLoading(true)
        interactor.showOnlyStarred = !interactor.showOnlyStarred
    }

    fun onItemClick(itemId: Long?) = navigator.navigateToItemComments(itemId)

    fun onItemLongClick(itemId: Long?): Boolean = when {
        itemId != null -> interactor.toggleItemStarredState(itemId)
        else -> false
    }

    fun destroy() = interactor.destroy()

    fun onMoreDataNeeded(currentMaxItem: Int) {
        if (interactor.loadMoreData(currentMaxItem)){
            viewBinder.showStatusMessage(R.string.status_loading_more_data)
            viewBinder.indicateDataLoading(true)
        }
    }
}

