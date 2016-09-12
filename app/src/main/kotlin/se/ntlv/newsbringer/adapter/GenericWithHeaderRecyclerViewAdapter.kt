package se.ntlv.newsbringer.adapter

import android.os.Trace
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.ViewGroup
import se.ntlv.newsbringer.adapter.GenericWithHeaderRecyclerViewAdapter.Type.HEADER
import se.ntlv.newsbringer.adapter.GenericWithHeaderRecyclerViewAdapter.Type.ROW
import se.ntlv.newsbringer.database.Identifiable


abstract class GenericWithHeaderRecyclerViewAdapter<S : Identifiable, A : S, B : S, HVH : ViewHolder, VH : ViewHolder>(
        val headerItemClass: Class<A>,
        val rowItemClass: Class<B>,
        val headerClass: Class<HVH>,
        val rowClass: Class<VH>) : GenericRecyclerViewAdapter<S, ViewHolder>() {

    enum class Type {
        HEADER, ROW;
    }

    abstract fun onCreateHeaderViewHolder(parent: ViewGroup?): ViewHolder
    abstract fun onCreateRowViewHolder(parent: ViewGroup?): ViewHolder

    abstract fun onBindHeaderViewHolder(viewHolder: HVH, data: A)
    abstract fun onBindRowViewHolder(viewHolder: VH, data: B)

    override fun onBindViewHolder(viewHolder: ViewHolder, item: S) {
        Trace.beginSection("on_bind_view_holder_with_header")
         when {
            headerItemClass.isInstance(item) -> onBindHeaderViewHolder(headerClass.cast(viewHolder), headerItemClass.cast(item))
            rowItemClass.isInstance(item) -> onBindRowViewHolder(rowClass.cast(viewHolder), rowItemClass.cast(item))
            else -> throw IllegalArgumentException("Object $item outside of type bounds.")
        }
        Trace.endSection()
    }

    override fun getItemViewType(position: Int): Int = (if (position == 0) HEADER else ROW).ordinal


    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) = when (Type.values()[viewType]) {
        HEADER -> onCreateHeaderViewHolder(parent)
        ROW -> onCreateRowViewHolder(parent)
    }

    fun findInDataSet(startDataPosition: Int,
                      predicate: (S) -> Boolean,
                      movementMethod: (Int) -> Int): Int? {

        val localDataRef = data ?: return null

        val startPos = startDataPosition - 1
        var pos = movementMethod(startPos)

        while (pos in 0..localDataRef.count - 1) {

            val item = localDataRef[pos]
            val targetFound = predicate(item)

            if (targetFound) {
                return pos + 1
            } else {
                pos = movementMethod(pos)
            }
        }
        return null
    }
}
