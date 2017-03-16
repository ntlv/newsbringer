package se.ntlv.newsbringer.comments

import android.util.Log
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription

class CommentsPresenter(val viewBinder: CommentsViewBinder, val interactor: CommentsInteractor) {

    private var viewReadyCalled = false
    private var subscriptions = CompositeSubscription()

    init {
        subscriptions.add(viewBinder.observeRefreshEvents().subscribe { refreshData() })
    }

    fun onViewReady() {
        if (viewReadyCalled) throw IllegalStateException("View called ready twice")
        viewReadyCalled = true
        viewBinder.indicateDataLoading(true)
        interactor.loadData().observeOn(AndroidSchedulers.mainThread())

        subscriptions.add(interactor.loadData().observeOn(AndroidSchedulers.mainThread()).subscribe {
            Log.d("CommentsPresenter", "Updating content, size ${it.size}")
            viewBinder.indicateDataLoading(false)
            viewBinder.updateContent(it)
        })
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
