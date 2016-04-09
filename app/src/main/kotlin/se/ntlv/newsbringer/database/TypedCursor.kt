package se.ntlv.newsbringer.database

import android.database.Cursor

abstract class TypedCursor<T>(val delegate: Cursor) : Cursor by delegate {

    abstract fun extract(raw: TypedCursor<T>): T

    fun getRow(pos: Int): T {
        moveToPositionOrThrow(pos)
        return extract(this)
    }

    override fun toString() = "${javaClass.simpleName}@${hashCode()} wrapping {${delegate.toString()}}"

}