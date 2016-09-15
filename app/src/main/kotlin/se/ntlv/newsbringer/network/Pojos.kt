package se.ntlv.newsbringer.network

import android.content.ContentValues
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.database.Database.CommentsTable
import se.ntlv.newsbringer.database.Database.PostTable
import java.util.*


sealed class RowItem : ParcelableIdentifiable {

    class NewsThreadUiData(val isStarred: Int,
                           val title: String,
                           val by: String,
                           val time: Long,
                           val score: Int,
                           val url: String,
                           override val id: Long,
                           val children: String,
                           val descendants: Long,
                           val ordinal: Int,
                           val text: String
    ) : RowItem() {

        companion object : AnkoLogger {
            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<NewsThreadUiData> = object : Parcelable.Creator<NewsThreadUiData> {
                override fun newArray(size: Int): Array<out NewsThreadUiData?> {
                    return arrayOfNulls(size)
                }

                override fun createFromParcel(source: Parcel): NewsThreadUiData {
                    info("Creating from parcel")
                    return NewsThreadUiData(source)
                }
            }
        }

        constructor(source: Parcel) : this(
                source.readInt(),
                source.readString(),
                source.readString(),
                source.readLong(),
                source.readInt(),
                source.readString(),
                source.readLong(),
                source.readString(),
                source.readLong(),
                source.readInt(),
                source.readString()
        )

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(isStarred)
            dest.writeString(title)
            dest.writeString(by)
            dest.writeLong(time)
            dest.writeInt(score)
            dest.writeString(url)
            dest.writeLong(id)
            dest.writeString(children)
            dest.writeLong(descendants)
            dest.writeInt(ordinal)
            dest.writeString(text)
        }

        override fun describeContents(): Int {
            return 0
        }

        constructor(cursor: Cursor) : this(
                cursor.getIntByName(PostTable.COLUMN_STARRED),
                cursor.getStringByName(PostTable.COLUMN_TITLE),
                cursor.getStringByName(PostTable.COLUMN_BY),
                cursor.getLongByName(PostTable.COLUMN_TIMESTAMP),
                cursor.getIntByName(PostTable.COLUMN_SCORE),
                cursor.getStringByName(PostTable.COLUMN_URL),
                cursor.getLongByName(PostTable.COLUMN_ID),
                cursor.getStringByName(PostTable.COLUMN_CHILDREN),
                cursor.getLongByName(PostTable.COLUMN_DESCENDANTS),
                cursor.getIntByName(PostTable.COLUMN_ORDINAL),
                cursor.getStringByName(PostTable.COLUMN_TEXT))


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as NewsThreadUiData

            if (isStarred != other.isStarred) return false
            if (title != other.title) return false
            if (by != other.by) return false
            if (time != other.time) return false
            if (score != other.score) return false
            if (url != other.url) return false
            if (id != other.id) return false
            if (children != other.children) return false
            if (descendants != other.descendants) return false
            if (ordinal != other.ordinal) return false
            if (text != other.text) return false

            return true
        }

        override fun hashCode(): Int {
            var result = isStarred
            result = 31 * result + title.hashCode()
            result = 31 * result + by.hashCode()
            result = 31 * result + time.hashCode()
            result = 31 * result + score
            result = 31 * result + url.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + children.hashCode()
            result = 31 * result + descendants.hashCode()
            result = 31 * result + ordinal
            result = 31 * result + text.hashCode()
            return result
        }
    }


    class CommentUiData(val position: Int,
                        val time: Long,
                        override val id: Long,
                        val by: String,
                        val kids: String,
                        val text: String,
                        val ancestorCount: Int
    ) : RowItem() {

        companion object : AnkoLogger {
            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<CommentUiData> {
                override fun newArray(size: Int): Array<out CommentUiData?> = arrayOfNulls(size)

                override fun createFromParcel(source: Parcel): CommentUiData {
                    info("Creating from parcel")
                    return CommentUiData(source)
                }

            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(position)
            dest.writeLong(time)
            dest.writeLong(id)
            dest.writeString(by)
            dest.writeString(kids)
            dest.writeString(text)
            dest.writeInt(ancestorCount)

        }

        override fun describeContents() = 0

        constructor(source: Parcel) : this(
                source.readInt(),
                source.readLong(),
                source.readLong(),
                source.readString(),
                source.readString(),
                source.readString(),
                source.readInt()
        )

        constructor(cursor: Cursor) : this(
                cursor.position,
                cursor.getLongByName(CommentsTable.COLUMN_TIME),
                cursor.getLongByName(CommentsTable.COLUMN_ID),
                cursor.getStringByName(CommentsTable.COLUMN_BY),
                cursor.getStringByName(CommentsTable.COLUMN_KIDS),
                cursor.getStringByName(CommentsTable.COLUMN_TEXT),
                cursor.getIntByName(CommentsTable.COLUMN_ANCESTOR_COUNT))

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as CommentUiData

            if (position != other.position) return false
            if (time != other.time) return false
            if (id != other.id) return false
            if (by != other.by) return false
            if (kids != other.kids) return false
            if (text != other.text) return false
            if (ancestorCount != other.ancestorCount) return false

            return true
        }

        override fun hashCode(): Int {
            var result = position
            result = 31 * result + time.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + by.hashCode()
            result = 31 * result + kids.hashCode()
            result = 31 * result + text.hashCode()
            result = 31 * result + ancestorCount
            return result
        }
    }
}


class NewsThread {

    constructor(itemId: Long) {
        id = itemId
    }

    constructor(newTitle: String, newStatus: Int, row: RowItem.NewsThreadUiData) {
        id = row.id
        ordinal = row.ordinal
        score = row.score
        time = row.time
        by = row.by
        title = newTitle
        kids = if (row.children.isNullOrBlank()) emptyArray() else row.children.split(',').map(String::toLong).toTypedArray()
        text = row.text
        type = ""
        url = row.url
        descendants = row.descendants
        starred = newStatus
    }

    var score: Int = 0
    var time: Long = 0
    var id: Long = 0
    var by: String? = null
    var title: String? = null
    var kids: Array<Long>? = null
    var text: String? = null
    var type: String? = null
    var url: String? = null
    var descendants: Long = 0

    var starred: Int = 0

    var ordinal: Int = -1

    fun toContentValues(): ContentValues {
        val cv = contentValuesOf(
                PostTable.COLUMN_ID to id,
                PostTable.COLUMN_SCORE to score,
                PostTable.COLUMN_TIMESTAMP to time,

                PostTable.COLUMN_BY to by.orEmpty(),
                PostTable.COLUMN_TITLE to title.orEmpty(),
                PostTable.COLUMN_CHILDREN to (kids?.joinToString(",") ?: ""),
                PostTable.COLUMN_TEXT to text.orEmpty(),
                PostTable.COLUMN_TYPE to type.orEmpty(),
                PostTable.COLUMN_URL to url.orEmpty(),

                PostTable.COLUMN_DESCENDANTS to descendants,

                PostTable.COLUMN_STARRED to starred,

                PostTable.COLUMN_ORDINAL to ordinal
        )
        return cv
    }
}

fun Boolean.toInt() = if (this) 1 else 0

class Comment {
    var parent: Long = 0
    var time: Long = 0
    var id: Long = 0
    var by: String? = null
    var kids: List<Long> = ArrayList()
    var text: String? = null
    var type: String? = null

    fun toContentValues(ordinal: Int, ancestorCount: Int, threadParent: Long): ContentValues {
        val varyingPart = if (ancestorCount > 0) {
            arrayOf(CommentsTable.COLUMN_PARENT_COMMENT to parent,
                    CommentsTable.COLUMN_PARENT to threadParent)
        } else {
            arrayOf(CommentsTable.COLUMN_PARENT to parent)
        }
        val cv = contentValuesOf(
                *varyingPart,
                CommentsTable.COLUMN_TIME to time,
                CommentsTable.COLUMN_ID to id,
                CommentsTable.COLUMN_BY to by.orEmpty(),
                CommentsTable.COLUMN_KIDS to kids.joinToString(","),
                CommentsTable.COLUMN_TEXT to text.orEmpty(),
                CommentsTable.COLUMN_TYPE to type.orEmpty(),
                CommentsTable.COLUMN_ORDINAL to ordinal,
                CommentsTable.COLUMN_ANCESTOR_COUNT to ancestorCount
        )
        return cv
    }
}
