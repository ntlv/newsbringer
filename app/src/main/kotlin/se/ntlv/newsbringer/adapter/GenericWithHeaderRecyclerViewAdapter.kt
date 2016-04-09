package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.ViewGroup


abstract class GenericWithHeaderRecyclerViewAdapter<T, HVH : ViewHolder, VH : ViewHolder>(
        val headerClass: Class<HVH>,
        val rowClass: Class<VH>) : GenericRecyclerViewAdapter<T, ViewHolder>(null) {

    private val HEADER_VIEW = 0
    private val COMMENTS_VIEW = 1

    abstract fun onCreateHeaderViewHolder(parent: ViewGroup?): ViewHolder
    abstract fun onCreateRowViewHolder(parent: ViewGroup?): ViewHolder

    abstract fun onBindHeaderViewHolder(viewHolder: HVH)
    abstract fun onBindRowViewHolder(viewHolder: VH, data: T)

    final override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (position == 0 && headerClass.isInstance(viewHolder)) {
            val castedHolder = headerClass.cast(viewHolder)
            onBindHeaderViewHolder(castedHolder)
            return
        }
        val translatedPosition = position - 1
        return super.onBindViewHolder(viewHolder, translatedPosition)
    }

    final override fun onBindViewHolder(viewHolder: ViewHolder, item: T) {
        if (rowClass.isInstance(viewHolder)) {
            val holder = rowClass.cast(viewHolder)
            onBindRowViewHolder(holder, item)
        } else {
            throw IllegalArgumentException("Cannot bind row with regular recycler view holder.")
        }
    }

    override fun getItemViewType(position: Int) = when (position) {
        0 -> HEADER_VIEW
        else -> COMMENTS_VIEW
    }

    override val actualItemCount: Int
        get() = data!!.count

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder {
        return when (viewType) {
            HEADER_VIEW -> onCreateHeaderViewHolder(parent)
            COMMENTS_VIEW -> onCreateRowViewHolder(parent)
            else -> throw IllegalArgumentException("Viewtype $viewType not supported")
        }
    }

    fun viewToDataPosition(viewPosition: Int) = viewPosition - 1
    fun dataToViewPosition(dataPosition: Int) = dataPosition + 1

    fun findInDataSet(startDataPosition: Int,
                      predicate: (T?) -> Boolean,
                      movementMethod: (Int) -> Int): Int? {

        val localDataRef = data ?: return null

        val startPos = viewToDataPosition(startDataPosition)
        var pos: Int = movementMethod(startPos)

        while (pos in 0..localDataRef.count - 1) {

            val item = localDataRef.getItem(pos)
            val targetFound = predicate(item)

            if (targetFound) {
                return dataToViewPosition(pos)
            } else {
                pos = movementMethod(pos)
            }
        }
        return null
    }
}