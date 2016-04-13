package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView

interface DataLoadingFacilitator {
    fun onMoreDataNeeded(currentMaxItem: Int): Unit
}

abstract class GenericRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(
        val facilitator: DataLoadingFacilitator?) : RecyclerView.Adapter<VH>() {

    abstract val actualItemCount: Int

    abstract protected val deltaUpdatingEnabled: Boolean

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

    fun updateContent(new: ObservableData<T>?) {
        if (new === data) {
            return
        }
        val old = data
        data = new

        if (deltaUpdatingEnabled) {
            applyDeltaUpdate(new, old)
        } else {
            notifyDataSetChanged()
        }

        old?.close()
    }

    private fun applyDeltaUpdate(new: ObservableData<T>?, old: ObservableData<T>?) {
        val end: Int
        val deltaUpdate: () -> Unit = when {
            new == null -> {
                end = 0
                { notifyItemRangeRemoved(0, old?.count ?: 0) }
            }
            old == null -> {
                end = 0
                val count = new.count
                { notifyItemRangeInserted(0, count) }
            }
            new.count == old.count -> {
                end = old.count - 1
                {}
            }
            new.count != old.count -> {
                end = Math.min(new.count, old.count) - 1
                val rangeStart = end + 1
                val rangeCount = Math.max(new.count, old.count) - Math.min(new.count, old.count);

                { notifyItemRangeInserted(rangeStart, rangeCount) }
            }
            else -> {
                throw IllegalArgumentException("Something is terribly wrong with input $new and $old")
            }
        }
        for (position in 0..end) {
            val newItem = new?.getItem(position)
            val oldItem = old?.getItem(position)

            if (newItem != oldItem) {
                notifyItemChanged(position)
            }
        }
        deltaUpdate()
    }

    var data: ObservableData<T>? = null
        private set

    fun toggleDynamicLoading() {
        if (facilitator == null) return
        shouldLoadDataDynamic = shouldLoadDataDynamic.not()
    }
}
