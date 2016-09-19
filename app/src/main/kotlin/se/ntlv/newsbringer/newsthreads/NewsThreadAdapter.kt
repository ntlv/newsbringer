package se.ntlv.newsbringer.newsthreads

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.find
import org.jetbrains.anko.onClick
import org.jetbrains.anko.onLongClick
import rx.Observable
import rx.Subscriber
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.adapter.starify
import se.ntlv.newsbringer.customviews.DateView
import se.ntlv.newsbringer.database.DataFrontPage
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData
import java.nio.BufferOverflowException

data class ProgressReport(val index: Int, val totalCount: Int)

class NewsThreadAdapter : GenericRecyclerViewAdapter<NewsThreadUiData, NewsThreadAdapter.ViewHolder> {


    private val mClickListener: (ViewHolder) -> Unit
    private val mLongClickListener: (ViewHolder) -> Boolean

    private val mObservers: MutableList<Subscriber<in ProgressReport>> = mutableListOf()

    constructor(clickListener: (ViewHolder) -> Unit,
                longClickListener: (ViewHolder) -> Boolean,
                seed: DataFrontPage?) : super(seed) {

        mClickListener = clickListener
        mLongClickListener = longClickListener
    }


    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_news_thread, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: NewsThreadAdapter.ViewHolder, position: Int) {
        val item = data!![position]
        val progress = ProgressReport(position, itemCount)

        mObservers.inPlaceFilteringForEach({ !it.isUnsubscribed }, { it.onNext(progress) })

        viewHolder.bind(item, mClickListener, mLongClickListener)
    }

    fun observeRenderProgress(): Observable<ProgressReport> =
            Observable.create<ProgressReport> { mObservers.add(it) }
                    .onBackpressureBuffer(10, { throw BufferOverflowException() })

    fun getConcreteData(): DataFrontPage? = data as? DataFrontPage

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val view = root
        val title = root.find<TextView>(R.id.title)
        val by = root.find<TextView>(R.id.by)
        val time = root.find<DateView>(R.id.time)
        val score = root.find<TextView>(R.id.score)
        var link: String? = null
        var id: Long? = null
        var isStarred: Int? = null
        val commentCount = root.find<TextView>(R.id.comment_count)
        val ordinal = root.find<TextView>(R.id.ordinal)

        fun bind(item: NewsThreadUiData, onClick: (ViewHolder) -> Unit, onLongClick: (ViewHolder) -> Boolean) {
            title.text = item.title.starify(item.isStarred)
            by.text = item.by
            time.text = item.time.toString()
            score.text = item.score.toString()
            link = item.url
            id = item.id
            isStarred = item.isStarred
            commentCount.text = item.descendants.toString()
            ordinal.text = item.ordinal.toString()

            view.onClick { onClick(this) }
            view.onLongClick { onLongClick(this) }
        }
    }

    fun destroy() = completeAllAndVerify(mObservers)
}

inline fun <T> MutableList<T>.inPlaceFilteringForEach(crossinline predicate: (T) -> Boolean, crossinline apply: (T) -> Unit) {
    val mutator = listIterator()
    while (mutator.hasNext()) {
        val item = mutator.next()
        if (predicate(item)) {
            apply(item)
        } else {
            mutator.remove()
        }
    }
}
