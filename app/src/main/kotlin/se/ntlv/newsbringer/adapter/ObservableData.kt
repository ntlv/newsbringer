package se.ntlv.newsbringer.adapter

import android.database.Cursor
import android.database.DataSetObserver
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.network.CommentUiData
import se.ntlv.newsbringer.network.NewsThreadUiData
import java.io.Closeable


interface ObservableData<T> : Closeable {

    val isValid: Boolean
    val count: Int

    var observer: DataSetObserver?

    fun getItemId(position: Int): Long
    fun getItem(position: Int): T

    fun hasContent() : Boolean
}

abstract class ObservableCursorData<T>(val data: Cursor) : ObservableData<T> {

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

    abstract fun getItem() : T

    override fun getItem(position: Int): T {
        data.moveToPositionOrThrow(position)
        return getItem()
    }

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
}

class NewsThreadData(data: Cursor) : ObservableCursorData<NewsThreadUiData>(data) {
    override fun getItem(): NewsThreadUiData {
        val isStarred = data.getInt(PostTable.COLUMN_STARRED)
        val title = data.getStringByName(PostTable.COLUMN_TITLE)
        val by = data.getStringByName(PostTable.COLUMN_BY)
        val time = data.getStringByName(PostTable.COLUMN_TIMESTAMP)
        val score = data.getStringByName(PostTable.COLUMN_SCORE)
        val url = data.getStringByName(PostTable.COLUMN_URL)
        val id = data.getLong(PostTable.COLUMN_ID)
        val children = data.getStringByName(PostTable.COLUMN_CHILDREN)
        val descendants = data.getLong(PostTable.COLUMN_DESCENDANTS)

        return NewsThreadUiData(isStarred, title, by, time, score, url, id, children, descendants)
    }
}

class CommentsData(data: Cursor) : ObservableCursorData<CommentUiData>(data) {

    override fun getItem(): CommentUiData {
        val pos = data.position
        val time = data.getLong(CommentsTable.COLUMN_TIME)
        val text = data.getStringByName(CommentsTable.COLUMN_TEXT)
        val id = data.getLong(CommentsTable.COLUMN_ID)
        val kids = data.getStringByName(CommentsTable.COLUMN_KIDS)
        val by = data.getStringByName(CommentsTable.COLUMN_BY)
        val ancestorCount = data.getInt(CommentsTable.COLUMN_ANCESTOR_COUNT)

        return CommentUiData(pos, time, id, by, kids, text, ancestorCount)
    }
}