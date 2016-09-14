package se.ntlv.newsbringer.newsthreads

import android.support.v7.util.DiffUtil
import se.ntlv.newsbringer.database.Identifiable

class DataDiffCallback<out T : Identifiable>(val mOld: List<T>?, val mNew: List<T>?) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = mOld?.size ?: 0
    override fun getNewListSize(): Int = mNew?.size ?: 0

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldId = mOld!![oldItemPosition].id
        val newId = mNew!![newItemPosition].id
        return oldId.equals(newId)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = mOld!![oldItemPosition]
        val new = mNew!![newItemPosition]
        return old.equals(new)
    }
}