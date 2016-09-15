package se.ntlv.newsbringer.comments

import rx.Subscription
import rx.android.schedulers.AndroidSchedulers.mainThread

class CommentsPresenter(val viewBinder: CommentsViewBinder, val interactor: CommentsInteractor) {

    private val refreshObserver: Subscription

    private var dataNeedsLoading = true
    private var subscription: Subscription? = null

    init {
        refreshObserver = viewBinder.observeRefreshEvents().subscribe { refreshData() }
    }

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        subscription?.unsubscribe()
        subscription = interactor.loadData().observeOn(mainThread()).subscribe {
            viewBinder.indicateDataLoading(false)
            viewBinder.updateContent(it)
            if (it.count <= 1 && dataNeedsLoading) {
                refreshData()
            }
            dataNeedsLoading = false
        }
    }

    fun refreshData() {
        viewBinder.indicateDataLoading(true)
        interactor.refreshComments()
    }

    fun onHeaderClick() = interactor.goToLink()

    fun onShareStoryClicked() = interactor.shareStory()

    fun onShareCommentsClicked() = interactor.shareComments()

    fun addToStarred() = interactor.addToStarred()

    fun destroy() {
        subscription?.unsubscribe()
        refreshObserver.unsubscribe()
    }
}
