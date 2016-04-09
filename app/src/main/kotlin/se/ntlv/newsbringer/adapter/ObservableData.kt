package se.ntlv.newsbringer.adapter

import android.database.DataSetObserver
import se.ntlv.newsbringer.database.TypedCursor
import se.ntlv.newsbringer.database.moveToPositionOrThrow
import java.io.Closeable


interface ObservableData<T> : Closeable {

    val isValid: Boolean
    val count: Int

    var observer: DataSetObserver?

    fun getItemId(position: Int): Long
    fun getItem(position: Int): T

    fun hasContent() : Boolean
}

class ObservableCursorData<T>(val data: TypedCursor<T>) : ObservableData<T> {

    private val internalObserver = NotifyingDataSetObserver()
    private var internalIsValid = true
    override val isValid: Boolean get() = internalIsValid

    private var idColumn: Int
    override var observer: DataSetObserver? = null
    override val count: Int get() = data.count

    init {
        idColumn = data.getColumnIndexOrThrow("_id")
        data.registerDataSetObserver(internalObserver)
    }

    override fun close() {
        data.unregisterDataSetObserver(internalObserver)
        data.close()
    }

    override fun getItemId(position: Int): Long {
        data.moveToPositionOrThrow(position)
        return data.getLong(idColumn)
    }

    override fun hasContent(): Boolean = data.count > 0

    override fun getItem(position: Int): T = data.getRow(position)

    private inner class NotifyingDataSetObserver : DataSetObserver() {
        override fun onChanged() {
            super.onChanged()
            internalIsValid = true
            observer?.onChanged()
        }

        override fun onInvalidated() {
            super.onInvalidated()
            internalIsValid = false
            observer?.onInvalidated()
        }
    }

    override fun toString() = "${javaClass.simpleName}@${hashCode()} wrapping {${data.toString()}}"
}