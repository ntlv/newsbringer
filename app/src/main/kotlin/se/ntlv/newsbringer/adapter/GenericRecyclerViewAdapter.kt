package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import se.ntlv.newsbringer.database.AdapterModelCollection
import se.ntlv.newsbringer.database.ParcelableIdentifiable

abstract class GenericRecyclerViewAdapter<T : ParcelableIdentifiable, VH : BindingViewHolder<T>>(seed: AdapterModelCollection<T>?) :
        RecyclerView.Adapter<VH>() {

    var data: AdapterModelCollection<T>? = seed
        set(value) {
            if (value === field) {
                return
            }
            field = value
            value?.diff?.dispatchUpdatesTo(this)
        }

    abstract override fun getItemViewType(position: Int): Int

    abstract override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): VH

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(data!![position])

    override fun getItemCount() = data?.size ?: 0

    override fun getItemId(position: Int) = data!![position].id
}

fun String.starify(isStarred: Int) = when (isStarred) {
    1 -> "\u2605 $this"
    else -> this
}
