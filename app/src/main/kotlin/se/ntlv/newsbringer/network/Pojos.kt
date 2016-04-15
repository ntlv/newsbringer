package se.ntlv.newsbringer.network

import android.content.ContentValues
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.database.PostTable
import java.util.*

data class NewsThreadUiData(val isStarred: Int,
                            val title: String,
                            val by: String,
                            val time: Long,
                            val score: Int,
                            val url: String,
                            val id: Long,
                            val children: String,
                            val descendants: Long,
                            val ordinal: Int,
                            val text: String
)

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
        val cv = ContentValues(12)
        cv.put(PostTable.COLUMN_ID, id)
        cv.put(PostTable.COLUMN_SCORE, score)
        cv.put(PostTable.COLUMN_TIMESTAMP, time)
        cv.put(PostTable.COLUMN_BY, by ?: "Unknown author")
        cv.put(PostTable.COLUMN_TITLE, title ?: "")
        cv.put(PostTable.COLUMN_CHILDREN, kids?.joinToString(",") ?: "")
        cv.put(PostTable.COLUMN_TEXT, text ?: "")
        cv.put(PostTable.COLUMN_TYPE, type ?: "Unknown type")
        cv.put(PostTable.COLUMN_URL, url ?: "Unkown URL")
        cv.put(PostTable.COLUMN_DESCENDANTS, descendants)

        cv.put(PostTable.COLUMN_STARRED, isStarredOverride.toInt())

        cv.put(PostTable.COLUMN_ORDINAL, ordinal)
        return cv
    }
}

fun Boolean.toInt() = if (this) 1 else 0

data class CommentUiData(val position: Int,
                         val time: Long,
                         val id: Long,
                         val by: String,
                         val kids: String,
                         val text: String,
                         val ancestorCount: Int
)

class Comment {
    var parent: Long = 0
    var time: Long = 0
    var id: Long = 0
    var by: String? = null
    var kids: List<Long> = ArrayList()
    var text: String? = null
    var type: String? = null

    fun toContentValues(ordinal: Int, ancestorCount: Int, threadParent: Long): ContentValues? {
        val cv: ContentValues
        if (ancestorCount > 0) {
            cv = ContentValues(10)
            cv.put(CommentsTable.COLUMN_PARENT_COMMENT, parent)
            cv.put(CommentsTable.COLUMN_PARENT, threadParent)
        } else {
            cv = ContentValues(9)
            cv.put(CommentsTable.COLUMN_PARENT, parent)
        }
        cv.put(CommentsTable.COLUMN_TIME, time)
        cv.put(CommentsTable.COLUMN_ID, id)
        cv.put(CommentsTable.COLUMN_BY, by ?: "Unknown author")
        cv.put(CommentsTable.COLUMN_KIDS, kids.joinToString(","))
        cv.put(CommentsTable.COLUMN_TEXT, text ?: "No text")
        cv.put(CommentsTable.COLUMN_TYPE, type ?: "Unknown type")
        cv.put(CommentsTable.COLUMN_ORDINAL, ordinal)
        cv.put(CommentsTable.COLUMN_ANCESTOR_COUNT, ancestorCount)
        return cv
    }
}
