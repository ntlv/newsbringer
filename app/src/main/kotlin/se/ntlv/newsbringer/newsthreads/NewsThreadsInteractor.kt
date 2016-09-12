package se.ntlv.newsbringer.newsthreads

import android.app.LoaderManager
import android.app.LoaderManager.LoaderCallbacks
import android.content.ContentResolver
import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.os.CancellationSignal
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.bundleOf
import org.jetbrains.anko.info
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.adapter.orCompute
import se.ntlv.newsbringer.database.NewsContentProvider
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.database.TypedCursor
import se.ntlv.newsbringer.database.TypedCursorLoader
import se.ntlv.newsbringer.network.AsyncDataService
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData
import java.util.*

class NewsThreadsInteractor(val context: Context, val loaderManager: LoaderManager) :
        LoaderCallbacks<TypedCursor<NewsThreadUiData>>, AnkoLogger {

    private val handledMoreLoadRequests: MutableSet<Int> = HashSet()

    private lateinit var onDataLoaded: ((TypedCursor<NewsThreadUiData>?) -> Unit)

    private var preservedCursorFromRestartedLoader: TypedCursor<NewsThreadUiData>? = null

    var showOnlyStarred = false
        set(b: Boolean) {
            field = b
            val loader = loaderManager.getLoader<TypedCursor<NewsThreadUiData>>(R.id.loader_front_page) as TypedCursorLoader<NewsThreadUiData>
            preservedCursorFromRestartedLoader = loader.unsafeGetCursor()
            val args = bundleOf(ARG_LOAD_ONLY_STARRED to field, ARG_TEXT_FILTER to filter)
            loaderManager.restartLoader(R.id.loader_front_page, args, this)
        }

    var filter = ""
        set(s: String) {
            field = s
            val loader = loaderManager.getLoader<TypedCursor<NewsThreadUiData>>(R.id.loader_front_page) as TypedCursorLoader<NewsThreadUiData>
            preservedCursorFromRestartedLoader = loader.unsafeGetCursor()
            val args = bundleOf(ARG_LOAD_ONLY_STARRED to showOnlyStarred, ARG_TEXT_FILTER to field)
            loaderManager.restartLoader(R.id.loader_front_page, args, this)
        }

    private val ARG_LOAD_ONLY_STARRED: String = "${javaClass.simpleName}:argLoadOnlyStarred"
    private val ARG_TEXT_FILTER: String = "${javaClass.simpleName}:argTextFilter"

    override fun onCreateLoader(id: Int, args: Bundle): Loader<TypedCursor<NewsThreadUiData>>? {

        val loadOnlyStarred: Boolean = args[ARG_LOAD_ONLY_STARRED]!! as Boolean
        val textFilter : String = args[ARG_TEXT_FILTER]!! as String

        val f = { previous: TypedCursor<NewsThreadUiData>?, resolver: ContentResolver, signal: CancellationSignal ->
            val query = NewsContentProvider.frontPageQuery(loadOnlyStarred, textFilter)
            val raw = resolver.query(query.url, query.projection, query.selection, query.selectionArgs, query.sorting, signal)
            val retVal = PostTable.PostTableCursor(raw)

            val old = previous.orCompute { preservedCursorFromRestartedLoader }
            preservedCursorFromRestartedLoader = null
            retVal.diff = DiffUtil.calculateDiff(GenericRecyclerViewAdapter.DataDiffCallback(old, retVal))
            retVal

        }
        return TypedCursorLoader(context, f)
    }

    override fun onLoadFinished(loader: Loader<TypedCursor<NewsThreadUiData>>?,
                                data: TypedCursor<NewsThreadUiData>?) = onDataLoaded(data)

    override fun onLoaderReset(loader: Loader<TypedCursor<NewsThreadUiData>>?) = onDataLoaded(null)

    fun downloadData() {
        handledMoreLoadRequests.clear()
        AsyncDataService.Action.FETCH_THREADS.asyncExecute(context, doFullWipe = true)
    }

    fun loadData(completion: (data: TypedCursor<NewsThreadUiData>?) -> Unit) {
        onDataLoaded = completion
        val args = bundleOf(ARG_LOAD_ONLY_STARRED to showOnlyStarred, ARG_TEXT_FILTER to filter)
        loaderManager.initLoader<TypedCursor<NewsThreadUiData>>(R.id.loader_front_page, args, this)
    }

    fun toggleItemStarredState(itemId: Long) = AsyncDataService.Action.TOGGLE_STARRED.asyncExecute(context, id = itemId)

    fun downloadMoreData(currentMaxItem: Int): Boolean {
        info("Download more data, $currentMaxItem")
        val alreadyHandled = handledMoreLoadRequests.contains(currentMaxItem)
        info("Request handled: $alreadyHandled")
        val willLoadData = !alreadyHandled && AsyncDataService.Action.FETCH_THREADS.asyncExecute(context, currentMax = currentMaxItem, doFullWipe = false)
        handledMoreLoadRequests.add(currentMaxItem)
        return willLoadData
    }
}

