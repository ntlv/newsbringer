package se.ntlv.newsbringer.database

import android.database.sqlite.SQLiteDatabase
import android.util.Log

public class PostTable {
    companion object {

        // Database table
        public val TABLE_NAME: String = "posts"

        public val COLUMN_ID: String = "_id"
        public val COLUMN_SCORE: String = "score"
        public val COLUMN_TIMESTAMP: String = "timestamp"
        public val COLUMN_BY: String = "by"
        public val COLUMN_CHILDREN: String = "children"
        public val COLUMN_TEXT: String = "text"
        public val COLUMN_TITLE: String = "title"
        public val COLUMN_TYPE: String = "type"
        public val COLUMN_URL: String = "url"
        public val COLUMN_ORDINAL: String = "ordinal"
        public val COLUMN_STARRED: String = "starred"

        val STARRED_SELECTION: String = "${COLUMN_STARRED}=?"
        val STARRED_SELECTION_ARGS: String = "1"
        val UNSTARRED_SELECTION_ARGS: String = "0"

        // Database creation SQL statement
        private val DATABASE_CREATE = "create table " + TABLE_NAME +
                "(" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_SCORE + " integer not null, " +
                COLUMN_TIMESTAMP + " integer not null, " +
                COLUMN_BY + " text not null, " +
                COLUMN_CHILDREN + " text not null, " +
                COLUMN_TEXT + " text not null, " +
                COLUMN_TITLE + " text not null, " +
                COLUMN_TYPE + " text not null, " +
                COLUMN_URL + " text not null, " +
                COLUMN_ORDINAL + " real not null, " +
                COLUMN_STARRED + " integer default 0 " +
                //+ " FOREIGN KEY (" + COLUMN_ID + ") REFERENCES "
                //+ ITAuthorTable.TABLE_NAME + " (" + ITAuthorTable.COLUMN_ID + ")"
                ");";

        private val LOG_TAG = "PostTable"

        public fun onCreate(database: SQLiteDatabase) {
            Log.d(LOG_TAG, "creating database")
            database.execSQL(DATABASE_CREATE)

        }

        public fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(LOG_TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
            database.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(database)
        }

        fun getDefaultProjection(): Array<String> = array(
                PostTable.COLUMN_SCORE,
                PostTable.COLUMN_TIMESTAMP,
                PostTable.COLUMN_BY,
                PostTable.COLUMN_TEXT,
                PostTable.COLUMN_TITLE,
                PostTable.COLUMN_URL,
                PostTable.COLUMN_ORDINAL,
                PostTable.COLUMN_ID,
                PostTable.COLUMN_CHILDREN,
                PostTable.COLUMN_STARRED
        )

        fun getCommentsProjection(): Array<String> = array(
                PostTable.COLUMN_ID,
                PostTable.COLUMN_TITLE,
                PostTable.COLUMN_TEXT,
                PostTable.COLUMN_BY,
                PostTable.COLUMN_TIMESTAMP,
                PostTable.COLUMN_SCORE,
                PostTable.COLUMN_URL,
                PostTable.COLUMN_STARRED
        )

        fun getOrdinalSortingString(): String = PostTable.COLUMN_ORDINAL + " DESC"
    }
}




