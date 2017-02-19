package se.ntlv.newsbringer.comments

import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers.mainThread
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit.SECONDS

class CommentsPresenter(val viewBinder: CommentsViewBinder, val interactor: CommentsInteractor) {

    private val refreshObserver: Subscription

    private var dataNeedsLoading = true
    private var dataSubscriptions = CompositeSubscription()

    init {
        refreshObserver = viewBinder.observeRefreshEvents().subscribe { refreshData() }
    }


    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        dataSubscriptions.clear()
        dataSubscriptions.add(interactor.loadData().observeOn(mainThread()).subscribe {
            dataNeedsLoading = false
            viewBinder.indicateDataLoading(false)
            viewBinder.updateContent(it)
        })
        dataSubscriptions.add(Observable.timer(3, SECONDS)
                .filter { dataNeedsLoading }
                .subscribe { refreshData() })
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
        dataSubscriptions.unsubscribe()
        refreshObserver.unsubscribe()
    }
}
