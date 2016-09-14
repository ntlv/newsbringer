package se.ntlv.newsbringer.database

import android.os.Parcelable
import android.support.v7.util.DiffUtil


interface IndexAccessible<out T> {
    operator fun get(position: Int): T
}

interface Identifiable {
    val id: Long
}

interface ParcelableIdentifiable : Identifiable, Parcelable

interface Diffable {
    val diff: DiffUtil.DiffResult?
}

class Data<out T : ParcelableIdentifiable>(val base: List<T>,
                                 override val diff: DiffUtil.DiffResult) : IndexAccessible<T>, Diffable {

    override fun get(position: Int): T = base[position]

    val count: Int
        get() = base.size
}
