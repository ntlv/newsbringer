package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView

interface DataLoadingFacilitator {
    fun onMoreDataNeeded(currentMaxItem: Int): Unit
}

abstract class GenericRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(
        val facilitator: DataLoadingFacilitator?) : RecyclerView.Adapter<VH>() {

    abstract val actualItemCount: Int

    var shouldLoadDataDynamic = facilitator != null

    override fun getItemCount() = when {
        data != null && data!!.isValid -> actualItemCount
        else -> 0
    }

    override fun getItemId(position: Int): Long = data?.getItemId(position) ?: -1

    abstract fun onBindViewHolder(viewHolder: VH, item: T)

    override fun onBindViewHolder(viewHolder: VH, position: Int): Unit {
        val item: T = data?.getItem(position) ?: throw IllegalStateException()
        val max = actualItemCount
        if (shouldLoadDataDynamic && itemCount > 9 && position >= max - 1) {
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
            field = newData
            old?.close()
            notifyDataSetChanged()
        }

    fun toggleDynamicLoading() {
        if (facilitator == null) return
        shouldLoadDataDynamic = shouldLoadDataDynamic.not()
    }
}
