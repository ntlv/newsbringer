package se.ntlv.newsbringer.newsthreads

import org.jetbrains.anko.AnkoLogger
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers.mainThread
import rx.subscriptions.CompositeSubscription
import se.ntlv.newsbringer.Navigator


class NewsThreadsPresenter(val viewBinder: NewsThreadsViewBinder,
                           val navigator: Navigator,
                           val interactor: NewsThreadsInteractor) : AnkoLogger {

    private val nonChangingSubscriptions: CompositeSubscription

    private var dataToBePresented: Subscription? = null
    private var starredOnly = false
    private var filter = ""

    init {
        val progress = viewBinder.observerPresentationProgress()
                .subscribe {
                    val (willLoad, message) = interactor.loadItemsAt(it.index)
                    if (willLoad) {
                        viewBinder.showStatusMessage("Loading rows $message")
                        viewBinder.indicateDataLoading(true)
                    }
                }

        val refresh = viewBinder.observeRefreshEvents().subscribe {
            refreshFrontPage()
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

    fun refreshFrontPage() {
        viewBinder.indicateDataLoading(true)
        interactor.refreshFrontPage()
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
        nonChangingSubscriptions.clear()
    }
}

