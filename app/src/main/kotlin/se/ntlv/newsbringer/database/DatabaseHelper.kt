package se.ntlv.newsbringer.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.squareup.sqlbrite.BriteDatabase
import com.squareup.sqlbrite.QueryObservable
import com.squareup.sqlbrite.SqlBrite
import rx.schedulers.Schedulers
import se.ntlv.newsbringer.network.NewsThreadUiData
import se.ntlv.newsbringer.thisShouldNeverHappen


class Database(ctx: Context) {

    private val mDb: BriteDatabase

    init {
        val helper = DatabaseHelper(ctx)
        val scheduler = Schedulers.io()
        mDb = SqlBrite.create().wrapDatabaseHelper(helper, scheduler)
    }

    private companion object {
        val DATABASE_NAME = "ycreader.db"
        val DATABASE_VERSION = 3
    }

    object PostTable {

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
        //Database creation SQL statement
        val DATABASE_CREATE = createTable(TABLE_NAME,
                COLUMN_ID int primaryKey,
                COLUMN_SCORE int nullAllowed,
                COLUMN_TIMESTAMP int nullAllowed,
                COLUMN_BY text nullAllowed,
                COLUMN_CHILDREN text nullAllowed,
                COLUMN_TEXT text nullAllowed,
                COLUMN_TITLE text nullAllowed,
                COLUMN_TYPE text nullAllowed,
                COLUMN_URL text nullAllowed,
                COLUMN_ORDINAL int nullAllowed,
                COLUMN_STARRED int defaultZero,
                COLUMN_DESCENDANTS int defaultZero
        )
    }

    object CommentsTable {

        // Database table
        val TABLE_NAME: String = "comment"
        //part of JSON blob
        val COLUMN_PARENT: String = "parent"
        val COLUMN_TIME: String = "time"
        val COLUMN_ID: String = "_id"
        val COLUMN_BY: String = "by"
        val COLUMN_KIDS_SIZE: String = "kids"
        val COLUMN_TEXT: String = "text"

        val COLUMN_TYPE: String = "type"
        //own schema
        val COLUMN_ORDINAL: String = "ordinal"
        val COLUMN_PARENT_COMMENT: String = "parent_comment"

        val COLUMN_ANCESTOR_COUNT: String = "ancestor_count"
        val DATABASE_CREATE = createTable(TABLE_NAME,
                COLUMN_ID int primaryKey,
                COLUMN_TIME int notNull,
                COLUMN_BY text notNull,
                COLUMN_PARENT int cascadeDelete(PostTable.TABLE_NAME, PostTable.COLUMN_ID),
                COLUMN_PARENT_COMMENT int cascadeDelete(CommentsTable.TABLE_NAME, CommentsTable.COLUMN_ID),
                COLUMN_ANCESTOR_COUNT int defaultZero,
                COLUMN_KIDS_SIZE int notNull,
                COLUMN_TEXT text notNull,
                COLUMN_TYPE text notNull,
                COLUMN_ORDINAL text notNull
        )
    }

    class DatabaseHelper(ctx: Context) : SQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {

        // Method is called during creation of the database
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(PostTable.DATABASE_CREATE)
            database.execSQL(CommentsTable.DATABASE_CREATE)
        }

        // Method is called during an upgrade of the database,
        // e.g. if you increase the database version
        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            database.execSQL("DROP TABLE IF EXISTS ${PostTable.TABLE_NAME}")
            database.execSQL("DROP TABLE IF EXISTS ${CommentsTable.TABLE_NAME}")
            onCreate(database)
        }

        override fun onConfigure(database: SQLiteDatabase) {
            database.setForeignKeyConstraintsEnabled(true) //needed for cascading deletes
        }
    }

    fun allStarredPosts(): List<NewsThreadUiData> {
        val select = "SELECT * FROM ${PostTable.TABLE_NAME}"
        val where = "WHERE ${PostTable.COLUMN_STARRED} = 1"

        return mDb.query("$select $where").use { cursor: Cursor ->
            val end = cursor.count -1
            (0..end).map {
                cursor.moveToPosition(it)
                NewsThreadUiData(cursor)
            }
        }
    }

    fun getFrontPage(starredOnly: Boolean = false, filter: String = ""): QueryObservable {

        val shouldMatchText = filter.isNotBlank()
        val conditions = when {
            starredOnly && shouldMatchText -> "${PostTable.COLUMN_STARRED}=1 and ${createFilter(filter)}"
            !starredOnly && shouldMatchText -> createFilter(filter)
            !starredOnly && !shouldMatchText -> "${PostTable.COLUMN_TITLE} is not null AND ${PostTable.COLUMN_TITLE} != ''"
            starredOnly && !shouldMatchText -> "${PostTable.COLUMN_STARRED}=1"
            else -> thisShouldNeverHappen()
        }

        val select = "SELECT * FROM ${PostTable.TABLE_NAME}"
        val where = "WHERE $conditions"
        val orderBy = "ORDER BY ${PostTable.COLUMN_ORDINAL} ASC"
        return mDb.createQuery(PostTable.TABLE_NAME, "$select $where $orderBy")
    }

    private fun createFilter(filter: String): String =
            arrayOf(PostTable.COLUMN_TITLE, PostTable.COLUMN_BY, PostTable.COLUMN_TEXT, PostTable.COLUMN_URL)
                    .map { "$it like '%$filter%'" }
                    .joinToString(" or ")

    fun insertNewsThreads(vararg items: NewsThreadUiData) {
        if (items.size == 1) {
            mDb.insert(PostTable.TABLE_NAME, items[0].toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
        } else {
            val things = items.map(NewsThreadUiData::toContentValues)
            insertManyThings(things)
        }
    }

    fun insertPlaceholders(placeholders: List<Pair<Long, Int>>) {
        val cvs = placeholders.map {
            val cv = ContentValues(2)
            cv.put(PostTable.COLUMN_ID, it.first)
            cv.put(PostTable.COLUMN_ORDINAL, it.second)
            cv
        }
        insertManyThings(cvs)
    }

    private fun insertManyThings(things: List<ContentValues>) {
        mDb.newTransaction().use {
            val success = things.map {
                mDb.insert(PostTable.TABLE_NAME, it, SQLiteDatabase.CONFLICT_REPLACE)
            }.none {
                it < 0
            }
            if (success) {
                it.markSuccessful()
            }
        }
    }

    fun getPostById(id: Long): QueryObservable {
        val select = "SELECT * FROM ${PostTable.TABLE_NAME}"
        val where = "WHERE ${PostTable.COLUMN_ID} = $id"

        return mDb.createQuery(PostTable.TABLE_NAME, "$select $where")
    }

    fun getCommentsForPost(postId: Long): QueryObservable {
        val select = "SELECT * FROM ${CommentsTable.TABLE_NAME}"
        val where = "WHERE ${CommentsTable.COLUMN_PARENT} = $postId"
        val orderBy = "ORDER BY ${CommentsTable.COLUMN_ORDINAL} ASC"
        return mDb.createQuery(CommentsTable.TABLE_NAME, "$select $where $orderBy")
    }

    fun hasCommentsForPostIdSync(postId: Long): Boolean {
        val select = "SELECT * FROM ${CommentsTable.TABLE_NAME}"
        val where = "WHERE ${CommentsTable.COLUMN_PARENT} = $postId"
        val orderBy = "ORDER BY ${CommentsTable.COLUMN_ORDINAL} ASC"
        mDb.query("$select $where $orderBy").use {
            return it.moveToFirst()
        }
    }

    fun getPostByIdSync(id: Long): NewsThreadUiData? {
        val select = "SELECT * FROM ${PostTable.TABLE_NAME}"
        val where = "WHERE ${PostTable.COLUMN_ID} = $id"

        mDb.query("$select $where").use {
            val hasData = it.moveToFirst()
            return when (hasData) {
                true -> NewsThreadUiData(it)
                false -> null
            }
        }
    }

    fun deleteFrontPage() {
        mDb.delete(PostTable.TABLE_NAME, null)
    }

    fun insertComment(reified: ContentValues) {
        mDb.insert(CommentsTable.TABLE_NAME, reified)
    }

    fun setStarred(id: Long, newStatus: Int) {
        val values = contentValuesOf(PostTable.COLUMN_STARRED to newStatus)
        val where = "${PostTable.COLUMN_ID} = ?"
        val updated = mDb.update(PostTable.TABLE_NAME, values, where, "$id")
        if (updated != 1) {
            throw RuntimeException("Updated $updated rows ")
        }
    }

    fun getPostByOrdinalSync(ordinal: Int): NewsThreadUiData? {
        val select = "SELECT * FROM ${PostTable.TABLE_NAME}"
        val where = "WHERE ${PostTable.COLUMN_ORDINAL} = $ordinal"

        mDb.query("$select $where").use {
            val hasData = it.moveToFirst()
            return when (hasData) {
                true -> NewsThreadUiData(it)
                false -> null
            }
        }
    }


}
