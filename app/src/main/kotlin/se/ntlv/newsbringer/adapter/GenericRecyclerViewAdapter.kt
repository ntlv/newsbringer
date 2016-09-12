package se.ntlv.newsbringer.adapter

import android.os.Trace
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import org.jetbrains.anko.AnkoLogger
import se.ntlv.newsbringer.database.Identifiable
import se.ntlv.newsbringer.database.TypedCursor

interface DataLoadingFacilitator {
    fun onMoreDataNeeded(currentMaxItem: Int): Unit
}

abstract class GenericRecyclerViewAdapter<T : Identifiable, VH : RecyclerView.ViewHolder>() : RecyclerView.Adapter<VH>(), AnkoLogger {

    var shouldLoadDataDynamic = false
    var facilitator: DataLoadingFacilitator? = null
        set(value) {
            field = value
            shouldLoadDataDynamic = true
        }

    protected var data: TypedCursor<T>? = null

    override fun getItemCount(): Int = data?.count ?: 0 //todo potentially add isValid (datasetobserver)

    override fun getItemId(position: Int): Long = data!![position].id

    abstract fun onBindViewHolder(viewHolder: VH, item: T)

    override fun onBindViewHolder(viewHolder: VH, position: Int) {
        Trace.beginSection("on_bind_view_holder_origin")
        val item: T = data!![position]
        val localCount = itemCount
        if (shouldLoadDataDynamic && localCount > 9 && position >= localCount - 1) {
            facilitator?.onMoreDataNeeded(localCount)
        }
        onBindViewHolder(viewHolder, item)
        Trace.endSection()
    }

    fun updateContent(new: TypedCursor<T>?) {
        if (new === data) {
            return
        }
        val old = data
        Trace.beginSection("calculate_diff")
        val res : DiffUtil.DiffResult
        if (new != null) {
            res = new.diff!!
        } else {
            res = DiffUtil.calculateDiff(DataDiffCallback(old, null))
        }
        Trace.endSection()
        data = new
        Trace.beginSection("dispatch_updates")
        res.dispatchUpdatesTo(this)
        Trace.endSection()
        old?.close()
    }

    class DataDiffCallback<out T : Identifiable>(val mOld: TypedCursor<T>?,
                                                 val mNew: TypedCursor<T>?) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = mOld?.count ?: 0
        override fun getNewListSize(): Int = mNew?.count ?: 0

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            Trace.beginSection("are_items_the_same")
            val oldId = mOld!![oldItemPosition].id
            val newId = mNew!![newItemPosition].id
            Trace.endSection()
            return oldId.equals(newId)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            Trace.beginSection("are_contents_the_same")
            val old = mOld!![oldItemPosition]
            val new = mNew!![newItemPosition]
            Trace.endSection()
            return old.equals(new)
        }
    }
}

inline fun <T> T?.orCompute(f :() -> T) : T = this ?: f()
