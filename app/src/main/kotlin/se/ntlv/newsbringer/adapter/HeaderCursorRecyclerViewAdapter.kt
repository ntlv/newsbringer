package se.ntlv.newsbringer.adapter

import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import se.ntlv.newsbringer.database.CommentsTable


public abstract class HeaderCursorRecyclerViewAdapter<
        HVH : RecyclerView.ViewHolder,
        VH : RecyclerView.ViewHolder
        >(HeaderClass : Class<HVH>, RowClass : Class<VH>) :
        CursorRecyclerViewAdapter<RecyclerView.ViewHolder>(null) {

    val mHeaderClass = HeaderClass
    val mRowClass = RowClass

    private val HEADER_VIEW = 0
    private val COMMENTS_VIEW = 1

    abstract fun onBindHeaderViewHolder(viewHolder: HVH)
    abstract fun onBindRowViewHolder(viewHolder: VH, cursor : Cursor)

    abstract fun onCreateHeaderViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder
    abstract fun onCreateRowViewHolder(parent: ViewGroup?): RecyclerView.ViewHolder


    /**
     * Main entry point called by Recycler view to bind data.
     */
    final override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (position == 0 && mHeaderClass.isInstance(viewHolder)) {
            val castedHolder = mHeaderClass.cast(viewHolder)
            onBindHeaderViewHolder(castedHolder)
            return
        }
        val translatedPosition = position - 1
        return super.onBindViewHolder(viewHolder, translatedPosition)
    }

    final override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, cursor: Cursor) {
        if(mRowClass.isInstance(viewHolder)) {
            val holder = mRowClass.cast(viewHolder)
            onBindRowViewHolder(holder, cursor)
        } else {
            throw IllegalArgumentException("Cannot bind row with regular recycler view holder.")
        }
    }

    override fun getItemViewType(position: Int) = when (position) {
        0 -> HEADER_VIEW
        else -> COMMENTS_VIEW
    }

    override val actualItemCount: Int
        get() = mCursor?.count ?: 0

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER_VIEW -> onCreateHeaderViewHolder(parent)
            COMMENTS_VIEW -> onCreateRowViewHolder(parent)
            else -> throw IllegalArgumentException("Viewtype $viewType not supported")
        }
    }

    fun viewToDataPosition(viewPosition: Int) = viewPosition - 1
    fun dataToViewPosition(dataPosition: Int) = dataPosition + 1

    fun findInDataSet(startDataPosition: Int,
                      predicate: (Cursor) -> Boolean,
                      movementMethod: (Cursor) -> Boolean): Int {
        val localCursor = mCursor ?: return 0
        val startPos = viewToDataPosition(startDataPosition)
        if (localCursor.moveToPosition(startPos).not()) {
            return 0
        }
        val columnIndex = localCursor.getColumnIndexOrThrow(CommentsTable.COLUMN_ANCESTOR_COUNT)
        while (predicate(localCursor).not()) {
            val ancestors = localCursor.getInt(columnIndex)
            when {
                ancestors < 1 && localCursor.position != startPos -> {
                    return dataToViewPosition(localCursor.position)
                }
                movementMethod(localCursor).not() -> {
                    return dataToViewPosition(startPos)
                }
            }
        }
        return dataToViewPosition(startPos)
    }
}