package se.ntlv.newsbringer.comments

import se.ntlv.newsbringer.database.TypedCursor
import se.ntlv.newsbringer.network.RowItem

class CommentsPresenter(val viewBinder: CommentsViewBinder, val interactor: CommentsInteractor) {

    var dataNeedsLoading = true

    fun onContentLoaded(data: TypedCursor<RowItem>?) {
        viewBinder.indicateDataLoading(false)
        viewBinder.updateContent(data)
        if (data?.count ?: 0 <= 1 && dataNeedsLoading) {
            refreshData()
        }
        dataNeedsLoading = false
    }

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        interactor.attach({ onContentLoaded(it) })
        interactor.loadData()
    }

    fun refreshData() {
        viewBinder.indicateDataLoading(true)
        interactor.refreshComments()
    }

    fun onHeaderClick() = interactor.goToLink()

    fun onShareStoryClicked() = interactor.shareStory()

    fun onShareCommentsClicked() = interactor.shareComments()

    fun addToStarred() = interactor.addToStarred()
}
