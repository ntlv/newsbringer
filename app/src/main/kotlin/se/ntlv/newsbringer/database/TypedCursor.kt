package se.ntlv.newsbringer.database

import android.database.ContentObserver
import android.database.Cursor
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.AnkoLogger


interface ContentObservable {
    fun registerContentObserver(var1: ContentObserver)

    fun unregisterContentObserver(var1: ContentObserver)
}

interface IndexAccessible<out T> {
    operator fun get(position : Int) : T
}

interface Identifiable {
    val id: Long
}

interface TypedCursor<out T : Identifiable> : IndexAccessible<T>, ContentObservable, Cursor, AnkoLogger {

    fun synthesizeModel(): T

    override operator fun get(position: Int) : T {
        moveToPositionOrThrow(position)
        return synthesizeModel()
    }

    var diff: DiffUtil.DiffResult?
}
