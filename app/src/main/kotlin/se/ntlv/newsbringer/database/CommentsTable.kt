package se.ntlv.newsbringer.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.support.v7.util.DiffUtil
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import se.ntlv.newsbringer.network.RowItem.CommentUiData

class CommentsTable {
    companion object : AnkoLogger {

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

        fun onCreate(database: SQLiteDatabase) {
            debug { "Creating database" }
            database.execSQL(DATABASE_CREATE)
        }

        fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            debug { "Upgrading database $oldVersion -> $newVersion, all existing data will be destroyed." }
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

    class CommentsTableCursor(base: Cursor) : TypedCursor<CommentUiData>, Cursor by base {
        override var diff: DiffUtil.DiffResult?
            get() = throw UnsupportedOperationException()
            set(value) = throw UnsupportedOperationException()


        override fun synthesizeModel(): CommentUiData {
            val pos = position
            val time = getLongByName(COLUMN_TIME)
            val text = getStringByName(COLUMN_TEXT)
            val id = getLongByName(COLUMN_ID)
            val kids = getStringByName(COLUMN_KIDS)
            val by = getStringByName(COLUMN_BY)
            val ancestorCount = getIntByName(COLUMN_ANCESTOR_COUNT)

            return CommentUiData(pos, time, id, by, kids, text, ancestorCount)
        }

    }
}




