package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView

interface DataLoadingFacilitator {
    fun onMoreDataNeeded(currentMaxItem: Int): Unit
}

abstract class GenericRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(
        val facilitator: DataLoadingFacilitator?) : RecyclerView.Adapter<VH>() {

    abstract fun actualItemCount(): Int

    abstract protected val deltaUpdatingEnabled: Boolean

    abstract fun viewToDataPosition(viewPosition: Int): Int
    abstract fun dataToViewPosition(dataPosition: Int): Int

    var shouldLoadDataDynamic = facilitator != null

    override fun getItemCount(): Int {
        val local = data
        val dataValid = local != null && local.isValid
        val count = if (dataValid) {
            actualItemCount()
        } else {
            0
        }
        return count
    }

    override fun getItemId(position: Int): Long = data?.getItemId(position) ?: -1

    abstract fun onBindViewHolder(viewHolder: VH, item: T)

    override fun onBindViewHolder(viewHolder: VH, position: Int): Unit {
        val item: T = data?.getItem(position) ?: throw IllegalStateException()
        val max = actualItemCount()
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

    private fun applyDeltaUpdate(new: ObservableData<T>?, old: ObservableData<T>?) = when {
        new == null -> {
            val start = dataToViewPosition(0);
            val count = old?.count ?: 0
            notifyItemRangeRemoved(start, count)
        }
        old == null -> {
            val rangeStart = dataToViewPosition(0);
            val rangeCount = new.count
            notifyItemRangeInserted(rangeStart, rangeCount)
        }
        new.count == old.count -> {
            val end = old.count - 1
            notifyChangedInRange(end, new, old)
        }
        new.count > old.count -> {
            val end = old.count - 1
            notifyChangedInRange(end, new, old)
            val rangeStart = dataToViewPosition(end + 1)
            val rangeCount = new.count - old.count;
            notifyItemRangeInserted(rangeStart, rangeCount)
        }
        new.count < old.count -> {
            val end = new.count - 1
            notifyChangedInRange(end, new, old)
            val rangeStart = dataToViewPosition(end + 1)
            val rangeCount = old.count - new.count;
            notifyItemRangeRemoved(rangeStart, rangeCount)
        }
        else -> {
            throw IllegalArgumentException("Something is terribly wrong with input $new and $old")
        }
    }

    private fun notifyChangedInRange(end :Int = -1,new: ObservableData<T>?, old: ObservableData<T>?) {
        for (position in 0..end) {
            val newItem = new?.getItem(position)
            val oldItem = old?.getItem(position)

            if (newItem != oldItem) {
                val translated = dataToViewPosition(position)
                notifyItemChanged(translated)
            }
        }
    }

    data class DeltaUpdate(val checkRangeEnd: Int = -1, val rangeStart: Int, val rangeCount: Int, val apply: () -> Unit)

    var data: ObservableData<T>? = null
        private set

    fun toggleDynamicLoading() {
        if (facilitator == null) return
        shouldLoadDataDynamic = shouldLoadDataDynamic.not()
    }
}
