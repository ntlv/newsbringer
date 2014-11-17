package se.ntlv.newsbringer.database

import android.database.sqlite.SQLiteDatabase
import android.util.Log

public class CommentsTable {
    class object {
        // Database table
        public val TABLE_NAME: String = "comment"

        public val COLUMN_PARENT: String = "parent"
        public val COLUMN_TIME: String = "time"
        public val COLUMN_ID: String = "_id"
        public val COLUMN_BY: String = "by"
        public val COLUMN_KIDS: String = "kids"
        public val COLUMN_TEXT: String = "text"
        public val COLUMN_TYPE: String = "type"
        public val COLUMN_ORDINAL: String = "ordinal"

        // Database creation SQL statement
        private val DATABASE_CREATE = "create table " + TABLE_NAME +
                "(" +
                COLUMN_ID + " integer primary key, " +
                COLUMN_TIME + " integer not null, " +
                COLUMN_BY + " text not null, " +
                COLUMN_PARENT + " text not null, " +
                COLUMN_KIDS + " text not null, " +
                COLUMN_TEXT + " text not null, " +
                COLUMN_TYPE + " text not null, " +
                COLUMN_ORDINAL + " real not null " +
                //+ " FOREIGN KEY (" + COLUMN_ID + ") REFERENCES "
                //+ ITAuthorTable.TABLE_NAME + " (" + ITAuthorTable.COLUMN_ID + ")"
                ");";

        private val LOG_TAG = javaClass<CommentsTable>().getSimpleName()

        public fun onCreate(database: SQLiteDatabase) {
            Log.d(LOG_TAG, "creating database")
            database.execSQL(DATABASE_CREATE)

        }

        public fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.w(LOG_TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
            database.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(database)
        }
    }
}




