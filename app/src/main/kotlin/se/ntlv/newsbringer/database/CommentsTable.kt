package se.ntlv.newsbringer.database

import android.database.sqlite.SQLiteDatabase
import android.util.Log

class CommentsTable {
    companion object {

        // Database table
        val TABLE_NAME: String = "comment"

        //part of JSON blob
        val COLUMN_PARENT: String = "parent"
        val COLUMN_TIME: String = "time"
        val COLUMN_ID: String = "_id"
        val COLUMN_BY: String = "by"
        val COLUMN_KIDS: String = "kids"
        val COLUMN_TEXT: String = "text"
        val COLUMN_TYPE: String = "type"

        //own schema
        val COLUMN_ORDINAL: String = "ordinal"
        val COLUMN_PARENT_COMMENT: String = "parent_comment"
        val COLUMN_ANCESTOR_COUNT: String = "ancestor_count"

        private val DATABASE_CREATE =
                createTable(TABLE_NAME,
                        COLUMN_ID int primaryKey,
                        COLUMN_TIME int notNull,
                        COLUMN_BY text notNull,
                        COLUMN_PARENT int cascadeDelete(PostTable.TABLE_NAME, PostTable.COLUMN_ID),
                        COLUMN_PARENT_COMMENT int cascadeDelete(CommentsTable.TABLE_NAME, CommentsTable.COLUMN_ID),
                        COLUMN_ANCESTOR_COUNT int defaultZero,
                        COLUMN_KIDS text notNull,
                        COLUMN_TEXT text notNull,
                        COLUMN_TYPE text notNull,
                        COLUMN_ORDINAL real notNull
                )

        // Database creation SQL statement
        private val DATABASE_CREATE_OLD = "create table " + TABLE_NAME +
                "(" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_TIME + " integer not null, " +
                COLUMN_BY + " text not null, " +
                COLUMN_PARENT + " integer REFERENCES " + PostTable.TABLE_NAME + "(" + PostTable.COLUMN_ID + ")" + " ON DELETE CASCADE, " +
                COLUMN_PARENT_COMMENT + " integer REFERENCES " + CommentsTable.TABLE_NAME + "(" + CommentsTable.COLUMN_ID + ")" + " ON DELETE CASCADE, " +
                COLUMN_ANCESTOR_COUNT + " integer default 0, " +
                COLUMN_KIDS + " text not null, " +
                COLUMN_TEXT + " text not null, " +
                COLUMN_TYPE + " text not null, " +
                COLUMN_ORDINAL + " real not null" +
                ");";

        private val LOG_TAG = CommentsTable::class.java.simpleName

        fun onCreate(database: SQLiteDatabase) {
            Log.d(LOG_TAG, "creating database")
            database.execSQL(DATABASE_CREATE)
        }

        fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(LOG_TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
            database.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(database)
        }

        fun getDefaultProjection(): Array<String> = arrayOf(
                CommentsTable.COLUMN_BY,
                CommentsTable.COLUMN_TEXT,
                CommentsTable.COLUMN_TIME,
                CommentsTable.COLUMN_ORDINAL,
                CommentsTable.COLUMN_ID,
                CommentsTable.COLUMN_ANCESTOR_COUNT,
                CommentsTable.COLUMN_KIDS
        )
    }
}




