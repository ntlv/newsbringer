package se.ntlv.newsbringer.newsthreads

import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.NewsThreadData
import se.ntlv.newsbringer.adapter.ObservableData
import se.ntlv.newsbringer.database.NewsContentProvider.Companion.CONTENT_URI_POSTS
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.network.DataPullPushService
import se.ntlv.newsbringer.network.NewsThreadUiData
import java.util.*

class NewsThreadsInteractor
(val context: Context,
 val loaderManager: LoaderManager) : LoaderManager.LoaderCallbacks<Cursor> {

    val handledMoreLoadRequests : MutableSet<Int> = HashSet()

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val selection = if (showOnlyStarred) PostTable.STARRED_SELECTION else null
        val selectionArgs = if (showOnlyStarred) arrayOf(PostTable.STARRED_SELECTION_ARGS) else null

        return CursorLoader(context, CONTENT_URI_POSTS,
                PostTable.getFrontPageProjection(), selection, selectionArgs, PostTable.getOrdinalSortingString())
    }

    var onDataLoaded: ((ObservableData<NewsThreadUiData>?) -> Unit) = { }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) = onDataLoaded(NewsThreadData(data!!))

    override fun onLoaderReset(loader: Loader<Cursor>?) = onDataLoaded(null)

    fun refreshData() {
        handledMoreLoadRequests.clear()
        DataPullPushService.startActionFetchThreads(context, doFullWipe = true)
    }

    fun loadData(completion: (data: ObservableData<NewsThreadUiData>?) -> Unit) {
        onDataLoaded = completion
        loaderManager.initLoader<Cursor>(R.id.loader_frontpage, null, this)
    }

    fun destroy() {
        loaderManager.destroyLoader(R.id.loader_frontpage)
    }

    var showOnlyStarred = false
        set(b: Boolean) {
            showOnlyStarred = b
            loaderManager.restartLoader(R.id.loader_frontpage, null, this)
        }

    fun toggleItemStarredState(itemId: Long): Boolean = DataPullPushService.startActionToggleStarred(context, itemId)

    fun loadMoreData(currentMaxItem: Int): Boolean {
        val alreadyHandled = handledMoreLoadRequests.contains(currentMaxItem)
        if (alreadyHandled || currentMaxItem >= DataPullPushService.MAX_THREAD_IDX ) {
            return false
        } else {
            handledMoreLoadRequests.add(currentMaxItem)
            DataPullPushService.startActionFetchThreads(context, currentMaxItem + 1, currentMaxItem + 10, false)
            return true
        }
    }
}