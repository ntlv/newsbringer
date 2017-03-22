package se.ntlv.newsbringer.comments

import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class CommentsPresenter(val viewBinder: CommentsViewBinder, val interactor: CommentsInteractor) {

    private var subscriptions = CompositeSubscription()

    init {
        val refreshObserver = viewBinder.observeRefreshEvents().subscribe { refreshData() }
        subscriptions.add(refreshObserver)
    }

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        interactor.loadData().observeOn(AndroidSchedulers.mainThread())

        val dataLoader = interactor.loadData()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    viewBinder.indicateDataLoading(false)
                    viewBinder.updateContent(it)
                }
        subscriptions.add(dataLoader)
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
        subscriptions.clear()
    }
}
