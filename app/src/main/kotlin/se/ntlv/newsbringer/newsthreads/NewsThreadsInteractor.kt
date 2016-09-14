package se.ntlv.newsbringer.newsthreads

import android.content.Context
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import rx.Observable
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.network.AsyncDataService
import se.ntlv.newsbringer.network.RowItem
import java.util.*


class NewsThreadsInteractor(val context: Context, val mDb: Database, seed: List<RowItem.NewsThreadUiData>?) : AnkoLogger {

    private val handledMoreLoadRequests: MutableSet<Int> = HashSet()

    fun downloadData(): Boolean {
        handledMoreLoadRequests.clear()
        return AsyncDataService.fetchThreads(context, 0, true)
    }

    private var mPreviousData: List<RowItem.NewsThreadUiData>? = seed

    fun loadData(starredOnly: Boolean, filter: String): Observable<Data<RowItem.NewsThreadUiData>> {
        return mDb.getFrontPage(starredOnly, filter)
                .mapToList { RowItem.NewsThreadUiData(it) }
                .map {
                    val old = mPreviousData
                    val new = it
                    val diff = DiffUtil.calculateDiff(DataDiffCallback(old, new))
                    mPreviousData = new
                    Data(it, diff)
                }
    }

    fun toggleItemStarredState(itemId: Long, starredStatus : Int) = AsyncDataService.toggleStarred(context, itemId, starredStatus)

    fun downloadMoreData(currentMaxItem: Int): Boolean {
        info("Download more data, $currentMaxItem")
        val alreadyHandled = handledMoreLoadRequests.contains(currentMaxItem)
        info("Request handled: $alreadyHandled")
        val willLoadData = !alreadyHandled && AsyncDataService.fetchThreads(context, currentMaxItem, false)
        handledMoreLoadRequests.add(currentMaxItem)
        return willLoadData
    }
}

