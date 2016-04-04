package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView

interface DataLoadingFacilitator {
    fun onMoreDataNeeded(currentMaxItem: Int): Unit
}

abstract class GenericRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(
        val facilitator: DataLoadingFacilitator?) : RecyclerView.Adapter<VH>() {

    abstract val actualItemCount: Int

    override fun getItemCount() = when {
        data != null && data!!.isValid -> actualItemCount
        else -> 0
    }

    override fun getItemId(position: Int): Long = data?.getItemId(position) ?: -1

    abstract fun onBindViewHolder(viewHolder: VH, item: T)

    override fun onBindViewHolder(viewHolder: VH, position: Int): Unit {
        val item: T = data?.getItem(position) ?: throw IllegalStateException()
        val max = actualItemCount
        if (position >= max - 1) {
            facilitator?.onMoreDataNeeded(max)
        }
        onBindViewHolder(viewHolder, item)
    }


    var data: ObservableData<T>? = null
        set(newData) {
            if (newData === field) {
                return
            }
            val old = field
//            val fieldIdxs = old?.count?.minus(1) ?: 0
//            val newIdxs = newData?.count?.minus(1) ?: 0

//            val iterEnd = Math.max(fieldIdxs, newIdxs)

            field = newData

//            for (idx in 0..iterEnd) {
//                if (idx in 0..fieldIdxs && idx in 0..newIdxs) {
//                    val fieldElem = field?.getItem(idx)
//                    val newItem = newData?.getItem(idx)
//                    if (fieldElem?.equals(newItem)?.not() ?: false) {
//                        notifyItemChanged(idx)
//                    }
//                } else {
//                    notifyItemChanged(idx)
//                }
//            }
            old?.close()
            notifyDataSetChanged()
        }
}
