package se.ntlv.newsbringer.newsthreads

import android.view.LayoutInflater
import android.view.ViewGroup
import rx.Emitter
import rx.Observable
import se.ntlv.newsbringer.R
import se.ntlv.newsbringer.adapter.BindingViewHolder
import se.ntlv.newsbringer.adapter.GenericRecyclerViewAdapter
import se.ntlv.newsbringer.application.createTrackedEmitterWithAutoRemove
import se.ntlv.newsbringer.database.DataFrontPage
import se.ntlv.newsbringer.network.NewsThreadUiData

class NewsThreadAdapter(val clickListener: (FrontpageHolder) -> Unit,
                        val longClickListener: (FrontpageHolder) -> Boolean,
                        seed: DataFrontPage?) : GenericRecyclerViewAdapter<NewsThreadUiData, BindingViewHolder<NewsThreadUiData>>(seed) {

    override fun getItemViewType(position: Int): Int = R.layout.header_item

    private val mEmitters : MutableList<Emitter<Int>> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BindingViewHolder<NewsThreadUiData> {
        val v = LayoutInflater.from(parent?.context).inflate(viewType, parent, false)
        return FrontpageHolder(v, clickListener, longClickListener)
    }

    override fun onBindViewHolder(holder: BindingViewHolder<NewsThreadUiData>, position: Int) {
        val item = data!![position]
        mEmitters.forEach { it.onNext(position) }
        holder.bind(item)
    }

    fun observePresentationPosition(): Observable<Int> = createTrackedEmitterWithAutoRemove(mEmitters)


    fun destroy() {
        mEmitters.forEach { it.onCompleted() }
        mEmitters.clear()
    }
}
