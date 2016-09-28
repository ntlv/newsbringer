package se.ntlv.newsbringer.customviews

import android.support.v7.util.DiffUtil
import se.ntlv.newsbringer.database.ParcelableIdentifiable

class DataDiffCallback<out T : ParcelableIdentifiable>(val mOld: List<T>?, val mNew: List<T>?) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = mOld?.size ?: 0
    override fun getNewListSize(): Int = mNew?.size ?: 0

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldId = mOld!![oldItemPosition].id
        val newId = mNew!![newItemPosition].id
        return oldId == newId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = mOld!![oldItemPosition]
        val new = mNew!![newItemPosition]
        return old == new
    }
}
