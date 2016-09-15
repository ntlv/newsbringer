package se.ntlv.newsbringer.newsthreads

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers.mainThread
import rx.subscriptions.CompositeSubscription
import se.ntlv.newsbringer.Navigator
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.thisShouldNeverHappen


class NewsThreadsPresenter(val viewBinder: NewsThreadsViewBinder,
                           val navigator: Navigator,
                           val interactor: NewsThreadsInteractor) : AnkoLogger {

    private val nonChangingSubscriptions: CompositeSubscription

    private var dataToBePresented: Subscription? = null
    private var starredOnly = false
    private var filter = ""

    init {
        val progress = viewBinder.observerPresentationProgress()
                .filter { it.second > 0.9f }
                .subscribe(
                        {
                            if (interactor.downloadMoreData(it.first)) {
                                viewBinder.showStatusMessage(R.string.status_loading_more_data)
                                viewBinder.indicateDataLoading(true)
                            }
                        },
                        { thisShouldNeverHappen(it.message) },
                        { info("OnComplete received") }
                )

        val refresh = viewBinder.observeRefreshEvents().subscribe {
            refreshData()
        }
        nonChangingSubscriptions = CompositeSubscription(progress, refresh)
    }

    fun onViewReady() {
        viewBinder.indicateDataLoading(true)
        dataToBePresented?.unsubscribe()
        dataToBePresented = interactor.loadData(starredOnly, filter).observeOn(mainThread()).subscribe {
            viewBinder.presentData(it)
            viewBinder.indicateDataLoading(false)
        }
    }

    fun refreshData() {
        viewBinder.indicateDataLoading(true)
        interactor.downloadData()
    }

    fun toggleShowOnlyStarred() {
        starredOnly = !starredOnly
        onViewReady()
    }

    fun onItemClick(itemId: Long): Unit = navigator.navigateToItemComments(itemId)

    fun onItemLongClick(itemId: Long, starredStatus: Int) = interactor.toggleItemStarredState(itemId, starredStatus)

    fun filter(newText: String?) {
        filter = newText ?: ""
        onViewReady()
    }

    fun destroy() {
        dataToBePresented?.unsubscribe()
        nonChangingSubscriptions.unsubscribe()
    }
}

