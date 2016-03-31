package se.ntlv.newsbringer.network

import android.content.ContentValues
import se.ntlv.newsbringer.database.CommentsTable
import se.ntlv.newsbringer.database.PostTable
import java.util.*

class NewsThread {

    var score: Int = 0
    var time: Long = 0
    var id: Long = 0
    var by: String? = null
    var title: String? = null
    var kids: LongArray? = null
    var text: String? = null
    var type: String? = null
    var url: String? = null

    val contentValue: ContentValues
        get() {
            val cv = ContentValues(9)
            cv.put(PostTable.COLUMN_ID, id)
            cv.put(PostTable.COLUMN_SCORE, score)
            cv.put(PostTable.COLUMN_TIMESTAMP, time)
            cv.put(PostTable.COLUMN_BY, by ?: "Unknown author")
            cv.put(PostTable.COLUMN_TITLE, title ?: "No title")
            cv.put(PostTable.COLUMN_CHILDREN, kids?.joinToString(",", "", "") ?: "")
            cv.put(PostTable.COLUMN_TEXT, text ?: "")
            cv.put(PostTable.COLUMN_TYPE, type ?: "Unknown type")
            cv.put(PostTable.COLUMN_URL, url ?: "Unkown URL")

            cv.put(PostTable.COLUMN_ORDINAL, calculateOrdinal(time, score))

            return cv
        }

    fun calculateOrdinal(time: Long, score: Int): Double {
        val unixTime = System.currentTimeMillis().div(1000)
        val hoursSinceSubmission = unixTime.minus(time).div(3600)
        val adjustedScore = (score.minus(1)).toDouble()
        return adjustedScore.div(Math.pow((hoursSinceSubmission.plus(2)).toDouble(), 1.8))
    }
//    data class Metadata(id: Long, text: String, title: String, by: String, time: String, score: String, link: String, kidCount: Long) {
//        val id = id
//        val text = text
//        val title = title
//        val by = by
//        val time = time
//        val score = score
//        val link = link
//        val kidCount = kidCount
//    }
}

class Comment {

    var parent: Long = 0
    var time: Long = 0
    var id: Long = 0
    var by: String? = null
    var kids: List<Long> = ArrayList()
    var text: String? = null
    var type: String? = null

    fun getAsContentValues(ordinal: Int,
                                  ancestorCount: Int,
                                  ancestorOrdinal: Double,
                                  threadParent: Long): ContentValues {
        val cv = ContentValues(8)
        if (ancestorCount > 0) {
            cv.put(CommentsTable.COLUMN_PARENT_COMMENT, parent)
            cv.put(CommentsTable.COLUMN_PARENT, threadParent)
        } else {
            cv.put(CommentsTable.COLUMN_PARENT, parent)
        }
        cv.put(CommentsTable.COLUMN_TIME, time)
        cv.put(CommentsTable.COLUMN_ID, id)
        cv.put(CommentsTable.COLUMN_BY, by ?: "Unknown author")
        cv.put(CommentsTable.COLUMN_KIDS, kids.joinToString(",", "", ""))
        cv.put(CommentsTable.COLUMN_TEXT, text ?: "No text")
        cv.put(CommentsTable.COLUMN_TYPE, type ?: "Unknown type")
        cv.put(CommentsTable.COLUMN_ORDINAL, calculateOrdinal(ordinal, ancestorOrdinal, ancestorCount))
        cv.put(CommentsTable.COLUMN_ANCESTOR_COUNT, ancestorCount)
        return cv
    }

    fun calculateOrdinal(ordinal: Int, ancestorOrdinal: Double, ancestorCount: Int): Double {
        return ancestorOrdinal + ordinal / Math.pow(10.0, ancestorCount.toDouble())
    }
}
