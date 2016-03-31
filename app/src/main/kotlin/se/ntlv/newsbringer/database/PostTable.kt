package se.ntlv.newsbringer.database

import android.database.sqlite.SQLiteDatabase
import android.util.Log

class PostTable {
    companion object {

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

        val STARRED_SELECTION: String = "$COLUMN_STARRED=?"
        val STARRED_SELECTION_ARGS: String = "1"
        val UNSTARRED_SELECTION_ARGS: String = "0"


        //Database creation SQL statement
        private val DATABASE_CREATE: String
            get() = createTable(TABLE_NAME,
                    COLUMN_ID int primaryKey,
                    COLUMN_SCORE int notNull,
                    COLUMN_TIMESTAMP int notNull,
                    COLUMN_BY text notNull,
                    COLUMN_CHILDREN text notNull,
                    COLUMN_TEXT text notNull,
                    COLUMN_TITLE text notNull,
                    COLUMN_TYPE text notNull,
                    COLUMN_URL text notNull,
                    COLUMN_ORDINAL real notNull,
                    COLUMN_STARRED int defaultZero
            )


        private val LOG_TAG = "PostTable"

        fun onCreate(database: SQLiteDatabase) {
            Log.d(LOG_TAG, "creating database")
            database.execSQL(DATABASE_CREATE)

        }

        fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(LOG_TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
            database.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(database)
        }

        fun getFrontPageProjection() = arrayOf(
                PostTable.COLUMN_SCORE,
                PostTable.COLUMN_TIMESTAMP,
                PostTable.COLUMN_BY,
                PostTable.COLUMN_TITLE,
                PostTable.COLUMN_URL,
                PostTable.COLUMN_ORDINAL,
                PostTable.COLUMN_ID,
                PostTable.COLUMN_CHILDREN,
                PostTable.COLUMN_STARRED
        )

        fun getCommentsProjection() = arrayOf(PostTable.COLUMN_ID,
                PostTable.COLUMN_TITLE,
                PostTable.COLUMN_TEXT,
                PostTable.COLUMN_BY,
                PostTable.COLUMN_TIMESTAMP,
                PostTable.COLUMN_SCORE,
                PostTable.COLUMN_URL,
                PostTable.COLUMN_STARRED
        )

        fun getOrdinalSortingString() = PostTable.COLUMN_ORDINAL + " DESC"
    }
}




