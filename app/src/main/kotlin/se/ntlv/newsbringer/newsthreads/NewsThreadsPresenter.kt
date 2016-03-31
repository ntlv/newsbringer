package se.ntlv.newsbringer.newsthreads

import android.database.Cursor
import se.ntlv.newsbringer.Navigator


class NewsThreadsPresenter(val viewBinder: NewsThreadsViewBinder,
                           val navigator: Navigator,
                           val interactor: NewsThreadsInteractor) {

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        interactor.loadData({ onDataLoaded(it) })
    }

    fun onDataLoaded(data: Cursor?) {
        viewBinder.data = data
        viewBinder.indicateDataLoading(false)
    }

    fun refreshData(needToIndicateLoading: Boolean) {
        viewBinder.indicateDataLoading(needToIndicateLoading)
        interactor.refreshData()
    }

    fun toggleShowOnlyStarred() {
        viewBinder.indicateDataLoading(true)
        interactor.showOnlyStarred = !interactor.showOnlyStarred
    }

    fun onItemClick(itemId: Long?) = navigator.navigateToItemComments(itemId)

    fun onItemLongClick(itemId: Long?) {
        if (itemId != null) {
            interactor.toggleItemStarredState(itemId)
        }
    }

    fun destroy() = interactor.destroy()
}

