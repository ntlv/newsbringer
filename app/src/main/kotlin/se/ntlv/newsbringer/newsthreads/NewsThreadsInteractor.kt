package se.ntlv.newsbringer.newsthreads

import android.app.LoaderManager
import android.content.ContentResolver
import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.os.CancellationSignal
import org.jetbrains.anko.AnkoLogger
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.ObservableCursorData
import se.ntlv.newsbringer.adapter.ObservableData
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.database.TypedCursor
import se.ntlv.newsbringer.database.TypedCursorLoader
import se.ntlv.newsbringer.network.DataPullPushService
import se.ntlv.newsbringer.network.NewsThreadUiData
import java.util.*

class NewsThreadsInteractor(val context: Context, val loaderManager: LoaderManager) :
        LoaderManager.LoaderCallbacks<TypedCursor<NewsThreadUiData>> {

    val handledMoreLoadRequests: MutableSet<Int> = HashSet()

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<TypedCursor<NewsThreadUiData>>? = FrontpageLoader(context, showOnlyStarred)

    var onDataLoaded: ((ObservableData<NewsThreadUiData>?) -> Unit) = { }
    var onDataFetchFailure: (String) -> Unit = {}

    override fun onLoadFinished(loader: Loader<TypedCursor<NewsThreadUiData>>?, data: TypedCursor<NewsThreadUiData>?) {
        val wrap = ObservableCursorData(data!!)
        onDataLoaded(wrap)
    }

    override fun onLoaderReset(loader: Loader<TypedCursor<NewsThreadUiData>>?) {
        onDataLoaded(null)
    }

    fun refreshData() {
        handledMoreLoadRequests.clear()
        DataPullPushService.startActionFetchThreads(context, doFullWipe = true)
    }

    fun loadData(completion: (data: ObservableData<NewsThreadUiData>?) -> Unit, onFail: (String) -> Unit) {
        onDataLoaded = completion
        onDataFetchFailure = onFail
        loaderManager.initLoader<TypedCursor<NewsThreadUiData>>(R.id.loader_frontpage, null, this)
    }

    fun destroy() {
        loaderManager.destroyLoader(R.id.loader_frontpage)
    }

    var showOnlyStarred = false
        set(b: Boolean) {
            field = b
            loaderManager.restartLoader(R.id.loader_frontpage, null, this)
        }

    fun toggleItemStarredState(itemId: Long): Boolean = DataPullPushService.startActionToggleStarred(context, itemId)

    fun loadMoreData(currentMaxItem: Int): Boolean {
        val alreadyHandled = handledMoreLoadRequests.contains(currentMaxItem)
        if (alreadyHandled || currentMaxItem >= DataPullPushService.MAX_THREAD_IDX ) {
            return false
        } else {
            handledMoreLoadRequests.add(currentMaxItem)
            DataPullPushService.startActionFetchThreads(context, currentMaxItem, false)
            return true
        }
    }
}

class FrontpageLoader(context: Context, showOnlyStarred: Boolean) : TypedCursorLoader<NewsThreadUiData>(context), AnkoLogger {

    val query = NewsContentProvider.frontPageQuery(showOnlyStarred)

    override fun query(resolver: ContentResolver, signal : CancellationSignal?): TypedCursor<NewsThreadUiData> {
        val raw = resolver.query(query.url, query.projection, query.selection, query.selectionArgs, query.sorting, signal)
        return PostTable.PostTableCursor(raw)
    }
}
