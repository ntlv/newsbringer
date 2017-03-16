package se.ntlv.newsbringer.network

import android.content.ContentValues
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable.Creator
import com.google.gson.JsonDeserializer
import se.ntlv.newsbringer.database.*
import se.ntlv.newsbringer.database.Database.CommentsTable
import se.ntlv.newsbringer.database.Database.PostTable
import java.io.IOException

sealed class RowItem : ParcelableIdentifiable

data class NewsThreadUiData(var isStarred: Int,
                            val title: String,
                            val by: String,
                            val time: Long,
                            val score: Int,
                            val url: String,
                            override val id: Long,
                            val children: List<Long>,
                            val descendants: Long,
                            var ordinal: Int,
                            val text: String) : RowItem() {

    companion object {
        @JvmField
        val CREATOR = object : Creator<NewsThreadUiData> {
            override fun newArray(size: Int) = arrayOfNulls<NewsThreadUiData>(size)

            override fun createFromParcel(source: Parcel) = NewsThreadUiData(
                    source.readInt(),
                    source.readString(),
                    source.readString(),
                    source.readLong(),
                    source.readInt(),
                    source.readString(),
                    source.readLong(),
                    source.createLongArray().asList(),
                    source.readLong(),
                    source.readInt(),
                    source.readString()
            )
        }

        fun createDeserializer() = JsonDeserializer { json, _, _ ->
            val j = json?.asJsonObject ?: throw IOException()

            val title = j.getAsJsonPrimitive("title").asString
            val by = j.getAsJsonPrimitive("by").asString
            val time = j.getAsJsonPrimitive("time").asLong
            val score = j.getAsJsonPrimitive("score").asInt
            val url = j.getAsJsonPrimitive("url")?.asString ?: ""
            val id = j.getAsJsonPrimitive("id").asLong
            val kids = j.getAsJsonArray("kids")?.map { it.asJsonPrimitive.asLong }?.filterNotNull() ?: listOf()
            val descendants = j.getAsJsonPrimitive("descendants")?.asLong ?: 0
            val text = j.getAsJsonPrimitive("text")?.asString ?: ""

            NewsThreadUiData(0, title, by, time, score, url, id, kids, descendants, 0, text)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(isStarred)
        dest.writeString(title)
        dest.writeString(by)
        dest.writeLong(time)
        dest.writeInt(score)
        dest.writeString(url)
        dest.writeLong(id)
        dest.writeLongArray(children.toLongArray())
        dest.writeLong(descendants)
        dest.writeInt(ordinal)
        dest.writeString(text)
    }

    constructor(cursor: Cursor) : this(
            cursor.getIntByName(PostTable.COLUMN_STARRED),
            cursor.getStringByName(PostTable.COLUMN_TITLE),
            cursor.getStringByName(PostTable.COLUMN_BY),
            cursor.getLongByName(PostTable.COLUMN_TIMESTAMP),
            cursor.getIntByName(PostTable.COLUMN_SCORE),
            cursor.getStringByName(PostTable.COLUMN_URL),
            cursor.getLongByName(PostTable.COLUMN_ID),
            cursor.getStringByName(PostTable.COLUMN_CHILDREN).split(",").filter(String::isNotBlank).map(String::toLong),
            cursor.getLongByName(PostTable.COLUMN_DESCENDANTS),
            cursor.getIntByName(PostTable.COLUMN_ORDINAL),
            cursor.getStringByName(PostTable.COLUMN_TEXT))

    fun toContentValues(): ContentValues = contentValuesOf(
            PostTable.COLUMN_ID to id,
            PostTable.COLUMN_SCORE to score,
            PostTable.COLUMN_TIMESTAMP to time,

            PostTable.COLUMN_BY to by.orEmpty(),
            PostTable.COLUMN_TITLE to title.orEmpty(),
            PostTable.COLUMN_CHILDREN to children.joinToString(","),
            PostTable.COLUMN_TEXT to text.orEmpty(),
            PostTable.COLUMN_TYPE to "story",
            PostTable.COLUMN_URL to url.orEmpty(),

            PostTable.COLUMN_DESCENDANTS to descendants,
            PostTable.COLUMN_STARRED to isStarred,
            PostTable.COLUMN_ORDINAL to ordinal
    )
}

data class CommentUiData(val parent: Long = 0,
                         val ordinal: Int,
                         val stringOrdinal: String,
                         val time: Long,
                         override val id: Long,
                         val by: String,
                         val kids: List<Long>,
                         val text: String,
                         val ancestorCount: Int) : RowItem() {

    constructor(cursor: Cursor) : this(
            cursor.getLongByName(CommentsTable.COLUMN_PARENT),
            cursor.position,
            cursor.getStringByName(CommentsTable.COLUMN_ORDINAL),
            cursor.getLongByName(CommentsTable.COLUMN_TIME),
            cursor.getLongByName(CommentsTable.COLUMN_ID),
            cursor.getStringByName(CommentsTable.COLUMN_BY),
            listOf(cursor.getLongByName(CommentsTable.COLUMN_KIDS_SIZE)),
            cursor.getStringByName(CommentsTable.COLUMN_TEXT),
            cursor.getIntByName(CommentsTable.COLUMN_ANCESTOR_COUNT))

    companion object {

        @JvmField
        val CREATOR = object : Creator<CommentUiData> {

            override fun newArray(size: Int) = arrayOfNulls<CommentUiData>(size)

            override fun createFromParcel(source: Parcel) =
                    CommentUiData(source.readLong(),
                            source.readInt(),
                            source.readString(),
                            source.readLong(),
                            source.readLong(),
                            source.readString(),
                            source.createLongArray().asList(),
                            source.readString(),
                            source.readInt()
                    )
        }

        fun createDeserializer() = JsonDeserializer { json, _, _ ->
            val j = json?.asJsonObject

            val parent = j?.getAsJsonPrimitive("parent")?.asLong ?: 0
            val time = j?.getAsJsonPrimitive("time")?.asLong ?: 0
            val id = j?.getAsJsonPrimitive("id")?.asLong ?: 0
            val by = j?.getAsJsonPrimitive("by")?.asString ?: "null-author"
            val kids = j?.getAsJsonArray("kids")?.map { it?.asJsonPrimitive?.asLong }?.filterNotNull() ?: emptyList()
            val text = j?.getAsJsonPrimitive("text")?.asString ?: "null-text"

            CommentUiData(parent, 0, "a", time, id, by, kids, text, 0)
        }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(parent)
        dest.writeInt(ordinal)
        dest.writeString(stringOrdinal)
        dest.writeLong(time)
        dest.writeLong(id)
        dest.writeString(by)
        dest.writeLongArray(kids.toLongArray())
        dest.writeString(text)
        dest.writeInt(ancestorCount)
    }

    fun toContentValues(ordinal: String, ancestorCount: Int, threadParent: Long): ContentValues {
        val varyingPart = if (ancestorCount > 0) {
            arrayOf(CommentsTable.COLUMN_PARENT_COMMENT to parent,
                    CommentsTable.COLUMN_PARENT to threadParent)
        } else {
            arrayOf(CommentsTable.COLUMN_PARENT to parent)
        }
        return contentValuesOf(
                *varyingPart,
                CommentsTable.COLUMN_TIME to time,
                CommentsTable.COLUMN_ID to id,
                CommentsTable.COLUMN_BY to by.orEmpty(),
                CommentsTable.COLUMN_KIDS_SIZE to kids.size,
                CommentsTable.COLUMN_TEXT to text.orEmpty(),
                CommentsTable.COLUMN_TYPE to "comment",
                CommentsTable.COLUMN_ORDINAL to ordinal,
                CommentsTable.COLUMN_ANCESTOR_COUNT to ancestorCount
        )
    }
}