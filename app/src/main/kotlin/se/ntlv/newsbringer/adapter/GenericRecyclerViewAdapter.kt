package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView
import java.util.*

interface DataLoadingFacilitator {
    fun onMoreDataNeeded(currentMaxItem: Int): Unit
}

abstract class GenericRecyclerViewAdapter<T, VH : RecyclerView.ViewHolder>(
        val facilitator: DataLoadingFacilitator?) : RecyclerView.Adapter<VH>() {

    abstract val actualItemCount: Int

    abstract protected val deltaUpdatingEnabled : Boolean

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

    fun updateContent(new : ObservableData<T>?) {
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
        val changedItems = ArrayList<Int>()
        if (new == null) {
            notifyItemRangeRemoved(0, old?.count ?: 0)
        } else if (old == null) {
            val count = new.count
            notifyItemRangeInserted(0, count)
        } else if (new.count == old.count) {
            for (i in 0..old.count - 1) {
                val newItem = new.getItem(i)
                val oldItem = old.getItem(i)
                if (!Objects.equals(newItem, oldItem)) {
                    changedItems.add(i)
                }
            }
            for (item in changedItems) {
                notifyItemChanged(item)
            }
        } else if (new.count > old.count) {
            for (i in 0..old.count - 1) {
                val newItem = new.getItem(i)
                val oldItem = old.getItem(i)
                if (!Objects.equals(newItem, oldItem)) {
                    changedItems.add(i)
                }
            }
            for (item in changedItems) {
                notifyItemChanged(item)
            }
            val newRangeStart = old.count
            val newRangeCount = new.count - old.count
            notifyItemRangeInserted(newRangeStart, newRangeCount)

        } else if (new.count < old.count) {
            for (i in 0..new.count - 1) {
                val newItem = new.getItem(i)
                val oldItem = old.getItem(i)
                if (!Objects.equals(newItem, oldItem)) {
                    changedItems.add(i)
                }
            }
            for (item in changedItems) {
                notifyItemChanged(item)
            }
            val removalStart = new.count
            val removalCount = old.count - new.count
            notifyItemRangeRemoved(removalStart, removalCount)
        }
    }


    var data: ObservableData<T>? = null
        private set

    fun toggleDynamicLoading() {
        if (facilitator == null) return
        shouldLoadDataDynamic = shouldLoadDataDynamic.not()
    }
}
