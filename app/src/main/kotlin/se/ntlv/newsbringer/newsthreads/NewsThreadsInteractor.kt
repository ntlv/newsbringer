package se.ntlv.newsbringer.newsthreads

import android.support.v7.util.DiffUtil
import android.util.Log
import org.jetbrains.anko.AnkoLogger
import rx.Observable
import se.ntlv.newsbringer.customviews.DataDiffCallback
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.database.DataFrontPage
import se.ntlv.newsbringer.database.Database
import se.ntlv.newsbringer.network.Io
import se.ntlv.newsbringer.network.NewsThreadUiData


class NewsThreadsInteractor(val mDb: Database,
                            seed: List<NewsThreadUiData>?) : AnkoLogger {

    private var mPreviousData = seed
    private var shouldLoad : BooleanArray

    init {
        val loadAtPos = if (seed != null && seed.size > Io.fetchInc ) seed.size - 1 else Io.fetchInc
        shouldLoad =  BooleanArray(500, { it == loadAtPos })
    }

    fun loadItemsAt(position: Int): Pair<Boolean, IntRange> {
        Log.v(loggerTag, "load items at $position with shouldLoad[$position]=${shouldLoad[position]}")
        val willLoad = shouldLoad[position]

        val range = position + 1..position + Io.fetchInc
        if (willLoad) {
            Log.v(loggerTag, "Loading more items since $position matched")
            shouldLoad[position] = false
            range.forEach {
                shouldLoad[it] = false
            }
            shouldLoad[range.endInclusive] = true
            Io.requestFetchThreads(range)
        }
        return willLoad to range
    }

    fun loadData(starredOnly: Boolean, filter: String): Observable<AdapterModelCollection<NewsThreadUiData>> {
        return mDb.getFrontPage(starredOnly, filter)
                .mapToList(::NewsThreadUiData)
                .map {
                    val old = mPreviousData
                    val new = it
                    val diff = DiffUtil.calculateDiff(DataDiffCallback(old, new))
                    mPreviousData = new
                    DataFrontPage(it, diff)
                }
    }

    fun toggleItemStarredState(itemId: Long, starredStatus: Int) = Io.requestToggleStarred(itemId, starredStatus)

    fun refreshFrontPage() {
        Io.requestFullWipe()
        shouldLoad = BooleanArray(500, { it >= Io.fetchInc })
    }
}

