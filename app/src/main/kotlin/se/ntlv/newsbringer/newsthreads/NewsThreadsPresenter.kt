package se.ntlv.newsbringer.newsthreads

import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.DataLoadingFacilitator
import se.ntlv.newsbringer.adapter.orCompute
import se.ntlv.newsbringer.database.TypedCursor
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData


class NewsThreadsPresenter(val viewBinder: NewsThreadsViewBinder,
                           val navigator: Navigator,
                           val interactor: NewsThreadsInteractor) : DataLoadingFacilitator {

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        interactor.loadData({ onDataLoaded(it)})
    }

    private fun onDataLoaded(data: TypedCursor<NewsThreadUiData>?) {
        viewBinder.presentData(data)
        viewBinder.indicateDataLoading(false)
    }

    fun refreshData() {
        viewBinder.indicateDataLoading(true)
        interactor.downloadData()
    }

    fun toggleShowOnlyStarred() {
        viewBinder.indicateDataLoading(true)
        viewBinder.toggleDynamicLoading()
        interactor.showOnlyStarred = !interactor.showOnlyStarred
    }

    fun onItemClick(itemId: Long) : Unit = navigator.navigateToItemComments(itemId)

    fun onItemLongClick(itemId: Long) = interactor.toggleItemStarredState(itemId)

    override fun onMoreDataNeeded(currentMaxItem: Int) {
        if (interactor.downloadMoreData(currentMaxItem)){
            viewBinder.showStatusMessage(R.string.status_loading_more_data)
            viewBinder.indicateDataLoading(true)
        }
    }

    fun filter(newText: String?) {
        viewBinder.indicateDataLoading(true)
        interactor.filter = newText.orCompute { "" }
    }
}

