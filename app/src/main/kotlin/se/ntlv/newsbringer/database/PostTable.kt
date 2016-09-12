package se.ntlv.newsbringer.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import se.ntlv.newsbringer.network.RowItem.NewsThreadUiData

class PostTable {
    companion object : AnkoLogger {

        // Database table
        val TABLE_NAME: String = "posts"

        val COLUMN_ID: String = "_id"
        val COLUMN_SCORE: String = "score"
        val COLUMN_TIMESTAMP: String = "timestamp"
        val COLUMN_BY: String = "by"
        val COLUMN_CHILDREN: String = "children"
        val COLUMN_TEXT: String = "text"
        val COLUMN_TITLE: String = "title"
        val COLUMN_TYPE: String = "type"
        val COLUMN_URL: String = "url"
        val COLUMN_ORDINAL: String = "ordinal"
        val COLUMN_STARRED: String = "starred"
        val COLUMN_DESCENDANTS: String = "descendants"

        val STARRED_SELECTION: String = "$COLUMN_STARRED=?"
        val STARRED_SELECTION_ARGS: String = "1"


        //Database creation SQL statement
        private val DATABASE_CREATE: String
            get() = createTable(
                    TABLE_NAME,
                    COLUMN_ID int primaryKey,
                    COLUMN_SCORE int nullAllowed,
                    COLUMN_TIMESTAMP int nullAllowed,
                    COLUMN_BY text nullAllowed,
                    COLUMN_CHILDREN text nullAllowed,
                    COLUMN_TEXT text nullAllowed,
                    COLUMN_TITLE text nullAllowed,
                    COLUMN_TYPE text nullAllowed,
                    COLUMN_URL text nullAllowed,
                    COLUMN_ORDINAL real nullAllowed,
                    COLUMN_STARRED int defaultZero,
                    COLUMN_DESCENDANTS int defaultZero
            )

        fun onCreate(database: SQLiteDatabase) {
            debug { "creating database" }
            database.execSQL(DATABASE_CREATE)

        }

        fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            debug { "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data" }
            database.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(database)
        }

        fun getOrdinalAndStarredProjection() = arrayOf(
                PostTable.COLUMN_ID,
                PostTable.COLUMN_ORDINAL,
                PostTable.COLUMN_STARRED
        )

        fun getFrontPageProjection() = arrayOf(
                PostTable.COLUMN_SCORE,
                PostTable.COLUMN_TIMESTAMP,
                PostTable.COLUMN_BY,
                PostTable.COLUMN_TITLE,
                PostTable.COLUMN_TEXT,
                PostTable.COLUMN_URL,
                PostTable.COLUMN_ORDINAL,
                PostTable.COLUMN_ID,
                PostTable.COLUMN_CHILDREN,
                PostTable.COLUMN_STARRED,
                PostTable.COLUMN_DESCENDANTS
        )

        fun getOrdinalSortingString() = PostTable.COLUMN_ORDINAL + " ASC"
    }

    class PostTableCursor(rawCursor: Cursor) : TypedCursor<NewsThreadUiData>, Cursor by rawCursor {
        override var diff: DiffUtil.DiffResult? = null

        override fun synthesizeModel(): NewsThreadUiData {

            val id = getLongByName(PostTable.COLUMN_ID)
            val score = getIntByName(PostTable.COLUMN_SCORE)
            val by = getStringByName(PostTable.COLUMN_BY)
            val children = getStringByName(PostTable.COLUMN_CHILDREN)
            val text = getStringByName(PostTable.COLUMN_TEXT)
            val title = getStringByName(PostTable.COLUMN_TITLE)
            val url = getStringByName(PostTable.COLUMN_URL)
            val ordinal = getIntByName(PostTable.COLUMN_ORDINAL)
            val starred = getIntByName(PostTable.COLUMN_STARRED)
            val descendants = getLongByName(PostTable.COLUMN_DESCENDANTS)
            val time = getLongByName(PostTable.COLUMN_TIMESTAMP)

            return NewsThreadUiData(starred, title, by, time, score, url, id, children, descendants, ordinal, text)
        }
    }
}

