package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.ViewGroup
import se.ntlv.newsbringer.adapter.GenericWithHeaderRecyclerViewAdapter.Type.HEADER
import se.ntlv.newsbringer.adapter.GenericWithHeaderRecyclerViewAdapter.Type.ROW
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.database.ParcelableIdentifiable
import se.ntlv.newsbringer.thisShouldNeverHappen


abstract class GenericWithHeaderRecyclerViewAdapter<S : ParcelableIdentifiable, A : S, B : S, HVH : ViewHolder, VH : ViewHolder>(
        seed: Data<S>?,
        val headerItemClass: Class<A>,
        val rowItemClass: Class<B>,
        val headerClass: Class<HVH>,
        val rowClass: Class<VH>) : GenericRecyclerViewAdapter<S, ViewHolder>(seed) {

    enum class Type {
        HEADER, ROW;
    }

    abstract fun onCreateHeaderViewHolder(parent: ViewGroup?): ViewHolder
    abstract fun onCreateRowViewHolder(parent: ViewGroup?): ViewHolder

    abstract fun onBindHeaderViewHolder(viewHolder: HVH, data: A)
    abstract fun onBindRowViewHolder(viewHolder: VH, data: B)

    override fun onBindViewHolder(viewHolder: ViewHolder, item: S) {
        when {
            headerItemClass.isInstance(item) -> onBindHeaderViewHolder(headerClass.cast(viewHolder), headerItemClass.cast(item))
            rowItemClass.isInstance(item) -> onBindRowViewHolder(rowClass.cast(viewHolder), rowItemClass.cast(item))
            else -> thisShouldNeverHappen("Object $item outside of type bounds.")
        }
    }

    override fun getItemViewType(position: Int): Int = (if (position == 0) HEADER else ROW).ordinal


    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = when (Type.values()[viewType]) {
        HEADER -> onCreateHeaderViewHolder(parent)
        ROW -> onCreateRowViewHolder(parent)
    }
}
