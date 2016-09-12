package se.ntlv.newsbringer.network

import android.content.ContentValues
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.database.Identifiable
import se.ntlv.newsbringer.database.PostTable
import se.ntlv.newsbringer.database.contentValuesOf
import java.util.*


sealed class RowItem : Identifiable {

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

    var score: Int = 0
    var time: Long = 0
    var id: Long = 0
    var by: String? = null
    var title: String? = null
    var kids: LongArray? = null
    var text: String? = null
    var type: String? = null
    var url: String? = null
    var descendants: Long = 0

    fun toContentValues(ordinal: Int, isStarredOverride: Boolean = false): ContentValues {
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

                PostTable.COLUMN_STARRED to isStarredOverride.toInt(),

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
