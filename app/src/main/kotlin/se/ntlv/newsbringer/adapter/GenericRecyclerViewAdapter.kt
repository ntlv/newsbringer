package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView
import org.jetbrains.anko.AnkoLogger
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.database.ParcelableIdentifiable

interface DataLoadingFacilitator {
    fun onMoreDataNeeded(currentMaxItem: Int): Unit
}

abstract class GenericRecyclerViewAdapter<T : ParcelableIdentifiable, VH : RecyclerView.ViewHolder>(seed: Data<T>?) :
        RecyclerView.Adapter<VH>(), AnkoLogger {

    var shouldLoadDataDynamic = false
    var facilitator: DataLoadingFacilitator? = null
        set(value) {
            field = value
            shouldLoadDataDynamic = true
        }

    var data: Data<T>? = seed
        private set

    override fun getItemCount(): Int = data?.count ?: 0 //todo potentially add isValid (datasetobserver)

    override fun getItemId(position: Int): Long = data!![position].id

    abstract fun onBindViewHolder(viewHolder: VH, item: T)

    override fun onBindViewHolder(viewHolder: VH, position: Int) {
        val item: T = data!![position]
        val localCount = itemCount
        if (shouldLoadDataDynamic && localCount > 9 && position >= localCount - 1) {
            facilitator?.onMoreDataNeeded(localCount)
        }
        onBindViewHolder(viewHolder, item)
    }

    fun updateContent(new: Data<T>) {
        if (new === data) {
            return
        }
        data = new
        new.diff.dispatchUpdatesTo(this)
    }

    fun findInDataSet(start: Int,
                      predicate: (T) -> Boolean,
                      movementMethod: (Int) -> Int): Int {

        val localDataRef = data ?: return start

        var currentPosition = movementMethod(start)

        while (currentPosition in 0..localDataRef.count) {

            if (predicate(localDataRef[currentPosition])) {
                return currentPosition
            } else {
                currentPosition = movementMethod(currentPosition)
            }
        }
        return start
    }
}

fun String?.starify(isStarred: Int) = when {
    this != null && isStarred == 1 -> "\u2605 " + this
    else -> this
}
