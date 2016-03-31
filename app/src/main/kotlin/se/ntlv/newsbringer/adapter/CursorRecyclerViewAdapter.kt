package se.ntlv.newsbringer.adapter

import android.database.Cursor
import android.database.DataSetObserver
import android.support.v7.widget.RecyclerView

abstract class CursorRecyclerViewAdapter<VH : RecyclerView.ViewHolder>() : RecyclerView.Adapter<VH>() {

    private val mDataSetObserver = NotifyingDataSetObserver()
    private var mDataValid: Boolean = false
    private var mRowIdColumn = 0
    var mCursor: Cursor? = null
        private set

    abstract val actualItemCount: Int

    override fun getItemCount() = when {
        mDataValid && mCursor != null -> actualItemCount
        else -> 0
    }

    override fun getItemId(position: Int): Long = when {
        mDataValid -> mCursor?.getLong(mRowIdColumn) ?: 0
        else -> 0
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(true)
    }

    abstract fun onBindViewHolder(viewHolder: VH, cursor: Cursor)

    fun Cursor?.moveToPositionOrThrow(pos: Int): Boolean =
            this?.moveToPosition(pos) ?: throw IllegalStateException("Attempted to move null cursor")

    override fun onBindViewHolder(viewHolder: VH, position: Int): Unit = when {
        !mDataValid -> throw IllegalStateException()
        else -> {
            mCursor.moveToPositionOrThrow(position)
            val safeCursor = mCursor ?: throw NullPointerException()
            onBindViewHolder(viewHolder, safeCursor)
        }
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor === mCursor) {
            return
        }
        mCursor?.unregisterDataSetObserver(mDataSetObserver)
        val oldCursor = mCursor

        mCursor = newCursor
        mCursor?.registerDataSetObserver(mDataSetObserver)
        mCursor?.getColumnIndexOrThrow("_id") ?: -1
        mDataValid = mCursor != null
        notifyDataSetChanged()
        oldCursor?.close()
    }

    private inner class NotifyingDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            mDataValid = true
            notifyDataSetChanged()
        }

        override fun onInvalidated() {
            super.onInvalidated()
            mDataValid = false
            notifyDataSetChanged()
        }
    }
}