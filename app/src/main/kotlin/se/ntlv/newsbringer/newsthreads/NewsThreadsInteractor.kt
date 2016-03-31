package se.ntlv.newsbringer.newsthreads

import android.app.LoaderManager
import android.content.Context
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.os.Bundle
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.network.DataPullPushService

class NewsThreadsInteractor
(val context: Context,
 val loaderManager: LoaderManager) : LoaderManager.LoaderCallbacks<Cursor> {

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val selection = if (showOnlyStarred) PostTable.STARRED_SELECTION else null
        val selectionArgs = if (showOnlyStarred) arrayOf(PostTable.STARRED_SELECTION_ARGS) else null

        return CursorLoader(context, NewsContentProvider.CONTENT_URI_POSTS,
                PostTable.getFrontPageProjection(), selection, selectionArgs, PostTable.getOrdinalSortingString())
    }

    var onDataLoaded : ((Cursor?) -> Unit) = { }

    override fun onLoadFinished(loader: Loader<Cursor>?, data: Cursor?) = onDataLoaded(data)

    override fun onLoaderReset(loader: Loader<Cursor>?) = onDataLoaded(null)

    fun refreshData() = DataPullPushService.startActionFetchThreads(context)

    fun loadData(completion : (data : Cursor?) -> Unit) {
        onDataLoaded = completion
        loaderManager.initLoader<Cursor>(R.id.loader_frontpage, null, this)
    }

    fun destroy() = loaderManager.destroyLoader(R.id.loader_frontpage)

    var showOnlyStarred = false
        set(b: Boolean) {
            showOnlyStarred = b
            loaderManager.restartLoader(R.id.loader_frontpage, null, this)
        }

    fun toggleItemStarredState(itemId: Long): Unit = DataPullPushService.startActionToggleStarred(context, itemId)
}