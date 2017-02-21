package se.ntlv.newsbringer.newsthreads

import android.content.Context
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.AnkoLogger
import rx.Observable
import se.ntlv.newsbringer.customviews.DataDiffCallback
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.database.DataFrontPage
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.network.Io
import se.ntlv.newsbringer.network.RowItem
import java.util.concurrent.atomic.AtomicInteger


class NewsThreadsInteractor(val context: Context,
                            val mDb: Database,
                            seed: List<RowItem.NewsThreadUiData>?) : AnkoLogger {

    private val mCurrentPos = AtomicInteger(-1)

    fun loadItemsAt(position: Int): Pair<Boolean, IntRange> {
        val range = position + 1..position + 10

        val willLoad = mCurrentPos.compareAndSet(position, range.last)
        if (willLoad) {
            Io.requestFetchThreads(range)
        }
        return willLoad to range
    }

    private var mPreviousData: List<RowItem.NewsThreadUiData>? = seed

    fun loadData(starredOnly: Boolean, filter: String): Observable<AdapterModelCollection<RowItem.NewsThreadUiData>> {
        return mDb.getFrontPage(starredOnly, filter)
                .mapToList { RowItem.NewsThreadUiData(it) }
                .map {
                    val old = mPreviousData
                    val new = it
                    mCurrentPos.compareAndSet(-1, new.size - 1)
                    val diff = DiffUtil.calculateDiff(DataDiffCallback(old, new))
                    mPreviousData = new
                    DataFrontPage(it, diff)
                }
    }

    fun toggleItemStarredState(itemId: Long, starredStatus: Int) = Io.requestToggleStarred(itemId, starredStatus)

    fun refreshFrontPage() {
        Io.requestFullWipe()
        mCurrentPos.set(0)
    }
}

