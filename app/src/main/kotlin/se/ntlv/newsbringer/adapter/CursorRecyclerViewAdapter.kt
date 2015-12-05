package se.ntlv.newsbringer.adapter

import android.database.Cursor
import android.database.DataSetObserver
import android.support.v7.widget.RecyclerView

public abstract class CursorRecyclerViewAdapter<VH : RecyclerView.ViewHolder>(cursor: Cursor?) : RecyclerView.Adapter<VH>() {

    private val mDataSetObserver: DataSetObserver
    private var mDataValid: Boolean = false
    private var mRowIdColumn = 0
    public var mCursor: Cursor? = null
        private set

    init {
        mCursor = cursor
        mDataValid = cursor != null
        mRowIdColumn = mCursor?.getColumnIndexOrThrow("_id") ?: -1
        mDataSetObserver = NotifyingDataSetObserver()
        mCursor?.registerDataSetObserver(mDataSetObserver)
    }

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

    public abstract fun onBindViewHolder(viewHolder: VH, cursor: Cursor)

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

    public fun swapCursor(newCursor: Cursor?): Cursor? {
        if (newCursor === mCursor) {
            return null
        }
        mCursor?.unregisterDataSetObserver(mDataSetObserver)
        val oldCursor = mCursor

        mCursor = newCursor
        mCursor?.registerDataSetObserver(mDataSetObserver)
        mCursor?.getColumnIndexOrThrow("_id") ?: -1
        mDataValid = mCursor != null
        notifyDataSetChanged()
        return oldCursor
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