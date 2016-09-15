package se.ntlv.newsbringer.database

import android.os.Parcelable
import android.support.v7.util.DiffUtil.DiffResult


interface IndexAccessible<out T> {
    operator fun get(position: Int): T
}

interface Identifiable {
    val id: Long
}

interface Diffable {
    val diff: DiffResult?
}

interface ParcelableIdentifiable : Identifiable, Parcelable

abstract class Data<out T : ParcelableIdentifiable>(val base: List<T>,
                                                    override val diff: DiffResult) : IndexAccessible<T>, Diffable {

    override fun get(position: Int): T = base[position]

    val count: Int
        get() = base.size
}
