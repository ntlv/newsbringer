package se.ntlv.newsbringer.newsthreads

import android.content.Context
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.AnkoLogger
import rx.Observable
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.database.DataFrontPage
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.network.AsyncDataService
import se.ntlv.newsbringer.network.AsyncDataService.Companion.requestFetchThread
import se.ntlv.newsbringer.network.RowItem
import java.util.concurrent.atomic.AtomicInteger


class NewsThreadsInteractor(val context: Context,
                            val mDb: Database,
                            seed: List<RowItem.NewsThreadUiData>?) : AnkoLogger {

    private val currentMax = AtomicInteger(-1)

    fun downloadItemNumber(currentPosition: Int): Pair<Boolean, IntRange> {
        val targetRange = currentPosition + 1..currentPosition + 10

        val willLoad = currentMax.compareAndRun(currentPosition, targetRange.last) { requestFetchThread(context, targetRange) }
        return willLoad to targetRange
    }

    private var mPreviousData: List<RowItem.NewsThreadUiData>? = seed

    fun loadData(starredOnly: Boolean, filter: String): Observable<Data<RowItem.NewsThreadUiData>> {
        return mDb.getFrontPage(starredOnly, filter)
                .mapToList { RowItem.NewsThreadUiData(it) }
                .map {
                    val old = mPreviousData
                    val new = it
                    currentMax.compareAndSet(-1, new.size - 1)
                    val diff = DiffUtil.calculateDiff(DataDiffCallback(old, new))
                    mPreviousData = new
                    DataFrontPage(it, diff)
                }
    }

    fun toggleItemStarredState(itemId: Long, starredStatus: Int) = AsyncDataService.requestToggleStarred(context, itemId, starredStatus)

    fun refreshFrontPage() {
        AsyncDataService.requestFullWipe(context)
        currentMax.set(0)
    }
}

fun AtomicInteger.compareAndRun(expected: Int, update: Int, f: () -> Boolean) =
        if (compareAndSet(expected, update)) f() else false

