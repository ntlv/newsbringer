package se.ntlv.newsbringer.database

import android.database.ContentObserver

interface LoadedClosableCollection<out T : Identifiable> {

    operator fun get(pos: Int): T

    fun registerContentObserver(observer: ContentObserver): Unit
    fun unregisterContentObserver(observer: ContentObserver): Unit
    fun close(): Unit

    val isClosed: Boolean
    val count: Int
}
