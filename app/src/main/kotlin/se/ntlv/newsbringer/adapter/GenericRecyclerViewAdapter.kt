package se.ntlv.newsbringer.adapter

import android.support.v7.widget.RecyclerView
import org.jetbrains.anko.AnkoLogger
import se.ntlv.newsbringer.database.Data
import se.ntlv.newsbringer.database.ParcelableIdentifiable

abstract class GenericRecyclerViewAdapter<T : ParcelableIdentifiable, VH : RecyclerView.ViewHolder>(seed: Data<T>?) :
        RecyclerView.Adapter<VH>(), AnkoLogger {

    var data: Data<T>? = seed
        private set

    override fun getItemCount(): Int = data?.count ?: 0

    override fun getItemId(position: Int): Long = data!![position].id

    fun updateContent(new: Data<T>) {
        if (new === data) {
            return
        }
        data = new
        new.diff.dispatchUpdatesTo(this)
    }
}

fun String?.starify(isStarred: Int) = when {
    this != null && isStarred == 1 -> "\u2605 " + this
    else -> this
}
