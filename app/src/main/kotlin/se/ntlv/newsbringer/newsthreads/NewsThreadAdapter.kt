package se.ntlv.newsbringer.newsthreads

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rx.Observable
import rx.Subscriber
import rx.subscriptions.Subscriptions
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.BindingViewHolder
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.database.DataFrontPage
import se.ntlv.newsbringer.network.RowItem
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData
import se.ntlv.newsbringer.network.TypesFactory
import se.ntlv.newsbringer.thisShouldNeverHappen
import java.nio.BufferOverflowException

class NewsThreadAdapter(clickListener: (FrontpageHolder) -> Unit,
                        longClickListener: (FrontpageHolder) -> Boolean,
                        seed: DataFrontPage?) : GenericRecyclerViewAdapter<NewsThreadUiData, BindingViewHolder<NewsThreadUiData>>(seed) {

    private class TypesFactoryImpl(private val clickListener: (FrontpageHolder) -> Unit,
                                   private val longClickListener: (FrontpageHolder) -> Boolean) : TypesFactory {
        override fun type(row: RowItem.CommentUiData): Int = thisShouldNeverHappen()

        override fun type(header: NewsThreadUiData): Int = R.layout.header_item

        override fun holder(type: Int, view: View): BindingViewHolder<RowItem.NewsThreadUiData> = when (type) {
            R.layout.header_item -> FrontpageHolder(view, clickListener, longClickListener)
            else -> thisShouldNeverHappen("$type cannot be resolved")
        }
    }

    private val factory = TypesFactoryImpl(clickListener, longClickListener)


    override fun getItemViewType(position: Int): Int {
        return data!![position].type(factory)
    }

    private val mObservers: MutableList<Subscriber<in ProgressReport>> = mutableListOf()


    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BindingViewHolder<NewsThreadUiData> {
        val v = LayoutInflater.from(parent?.context).inflate(viewType, parent, false)
        return factory.holder(viewType, v)
    }

    override fun onBindViewHolder(holder: BindingViewHolder<NewsThreadUiData>, position: Int) {
        val item = data!![position]
        val progress = ProgressReport(position, itemCount)
        mObservers.forEach { it.onNext(progress) }

        holder.bind(item)
    }

    fun observeRenderProgress(): Observable<ProgressReport> =
            Observable.create<ProgressReport> { it: Subscriber<in ProgressReport> ->
                mObservers.add(it)
                it.add(Subscriptions.create { mObservers.remove(it) })
            }.onBackpressureBuffer(10, { throw BufferOverflowException() })

    fun destroy() {
        completeAllAndVerify(mObservers)
        mObservers.clear()
    }
}
