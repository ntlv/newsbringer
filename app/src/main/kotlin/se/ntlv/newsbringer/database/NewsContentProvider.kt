package se.ntlv.newsbringer.database

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.os.CancellationSignal
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.verbose
import se.ntlv.newsbringer.database.PostTable.Companion.COLUMN_BY
import se.ntlv.newsbringer.database.PostTable.Companion.COLUMN_STARRED
import se.ntlv.newsbringer.database.PostTable.Companion.COLUMN_TEXT
import se.ntlv.newsbringer.database.PostTable.Companion.COLUMN_TITLE
import se.ntlv.newsbringer.database.PostTable.Companion.COLUMN_URL


class NewsContentProvider : ContentProvider(), AnkoLogger {

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        throw UnsupportedOperationException()
    }

    override fun bulkInsert(uri: Uri?, values: Array<out ContentValues>): Int {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database.writableDatabase

        val tableName = when (uriType) {
            POSTS -> PostTable.TABLE_NAME
            COMMENTS -> CommentsTable.TABLE_NAME
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }

        sqlDB.beginTransaction()
        try {
            values.forEach {
                val result = sqlDB.replace(tableName, null, it)
                if (result < 0) throw SQLException("Unable to insert all rows")
            }
            verbose { "Successfully inserted ${values.size} rows." }
            sqlDB.setTransactionSuccessful()
            return values.size
        } finally {
            sqlDB.endTransaction()
            context.contentResolver.notifyChange(uri, null)
        }
    }

    override fun onCreate(): Boolean {
        verbose { "Content provider created" }
        database = DatabaseHelper(context)
        return true
    }

    lateinit var database: DatabaseHelper


    override fun query(uri: Uri, projection: Array<String>, selection: String?, selectionArguments: Array<String>?, sortOrder: String?, signal: CancellationSignal?): Cursor {

        val queryBuilder = SQLiteQueryBuilder()
        var limit: String? = null
        when (sUriMatcher.match(uri)) {
            POST_ID -> {
                queryBuilder.appendWhere(PostTable.COLUMN_ID + "=" + uri.lastPathSegment)
                queryBuilder.tables = PostTable.TABLE_NAME
            }
            COMMENTS_ID -> {
                queryBuilder.appendWhere(CommentsTable.COLUMN_ID + "=" + uri.lastPathSegment)
                queryBuilder.tables = CommentsTable.TABLE_NAME
            }
            POSTS -> queryBuilder.tables = PostTable.TABLE_NAME
            COMMENTS -> queryBuilder.tables = CommentsTable.TABLE_NAME
            POSTS_W_LIMIT -> {
                queryBuilder.tables = PostTable.TABLE_NAME
                limit = uri.lastPathSegment
            }
        }
        val db = database.readableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArguments, null, null, sortOrder, limit, signal)
        cursor.setNotificationUri(context.contentResolver, uri)
        verbose { "Query for {$selection} resulted in ${cursor?.count} rows" }
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues): Uri {
        verbose { "Inserting ${contentValues.valueSet().toString().substring(0..115)}" }

        val uriType = sUriMatcher.match(uri)
        val sqlDB = database.writableDatabase

        val rowId = when (uriType) {
            POSTS -> sqlDB.replace(PostTable.TABLE_NAME, null, contentValues)
            COMMENTS -> sqlDB.replace(CommentsTable.TABLE_NAME, null, contentValues)
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        if (rowId == -1L) throw IllegalArgumentException("Unable to insert $contentValues into $uri")

        val id = ContentUris.withAppendedId(CONTENT_URI_POSTS, rowId)
        context.contentResolver.notifyChange(uri, null)
        return id
    }

    override fun delete(uri: Uri, selection: String?, selectionArguments: Array<String>?): Int {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val id = uri.lastPathSegment

        val rowsDeleted = when (uriType) {
            POSTS -> sqlDB.delete(PostTable.TABLE_NAME, selection, selectionArguments)
            POST_ID -> handleItemDelete(PostTable.TABLE_NAME, PostTable.COLUMN_ID, id, selection, selectionArguments)
            COMMENTS -> sqlDB.delete(CommentsTable.TABLE_NAME, selection, selectionArguments)
            POST_ID -> handleItemDelete(CommentsTable.TABLE_NAME, CommentsTable.COLUMN_ID, id, selection, selectionArguments)

            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        context.contentResolver.notifyChange(uri, null)
        return rowsDeleted
    }

    private fun handleItemDelete(tableName: String,
                                 idColumn: String,
                                 id: String,
                                 selection: String?,
                                 selectionArgs: Array<String>?): Int {
        val sqlDb = database.writableDatabase
        val where = idColumn + "=" + id + (if (selection.isNullOrEmpty()) " and " + selection else "")
        return sqlDb.delete(tableName, where, selectionArgs)
    }

    override fun update(uri: Uri, values: ContentValues, selection: String, selectionArgs: Array<String>?): Int {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database.writableDatabase
        val rowsUpdated: Int = when (uriType) {
            POSTS -> sqlDB.update(PostTable.TABLE_NAME, values, selection, selectionArgs)
            COMMENTS -> sqlDB.update(CommentsTable.TABLE_NAME, values, selection, selectionArgs)
            POST_ID -> handleItemUpdate(selection, selectionArgs, values, PostTable.TABLE_NAME, PostTable.COLUMN_ID, uri.lastPathSegment)
            COMMENTS_ID -> handleItemUpdate(selection, selectionArgs, values, CommentsTable.TABLE_NAME, CommentsTable.COLUMN_ID, uri.lastPathSegment)
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        context.contentResolver.notifyChange(uri, null)
        return rowsUpdated
    }

    private fun handleItemUpdate(selection: String,
                                 selectionArgs: Array<String>?,
                                 values: ContentValues,
                                 tableName: String,
                                 idColumn: String,
                                 itemId: String): Int {
        val sqlDb = database.writableDatabase
        val where = "$idColumn=$itemId" + if (selection.isNotBlank()) " and $selection" else ""
        return sqlDb.update(tableName, values, where, selectionArgs)
    }

    companion object {

        private val POSTS = 10
        private val POST_ID = 20
        private val POSTS_W_LIMIT = 21

        private val COMMENTS = 30
        private val COMMENTS_ID = 40

        private val AUTHORITY = "se.ntlv.newsbringer.database.NewsContentProvider"

        private val CONTENT_POSTS = "posts"
        val CONTENT_URI_POSTS: Uri = Uri.parse("content://$AUTHORITY/$CONTENT_POSTS")

        private val CONTENT_POSTS_LIMIT = "$CONTENT_POSTS/limit"
        fun CONTENT_URI_POSTS_W_LIMIT(limit: Int): Uri = Uri.parse("content://$AUTHORITY/$CONTENT_POSTS_LIMIT/$limit")

        private val CONTENT_COMMENTS = "comments"

        val CONTENT_URI_COMMENTS: Uri = Uri.parse("content://$AUTHORITY/$CONTENT_COMMENTS")

        var sUriMatcher: UriMatcher

        init {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher.addURI(AUTHORITY, CONTENT_POSTS, POSTS)
            sUriMatcher.addURI(AUTHORITY, "$CONTENT_POSTS/#", POST_ID)
            sUriMatcher.addURI(AUTHORITY, "$CONTENT_POSTS_LIMIT/#", POSTS_W_LIMIT)
            sUriMatcher.addURI(AUTHORITY, CONTENT_COMMENTS, COMMENTS)
            sUriMatcher.addURI(AUTHORITY, "$CONTENT_COMMENTS/#", COMMENTS_ID)
        }

        fun frontPageQuery(starredOnly: Boolean, textMatch: String): Query {
            val projection = PostTable.getFrontPageProjection()

            val shouldMatchText = textMatch.isNotBlank()

            val likeMatcher = "(" + arrayOf(COLUMN_TITLE, COLUMN_BY, COLUMN_TEXT, COLUMN_URL).joinToString(" like '%$textMatch%' or ") + " like '%$textMatch%')"

            val selection = when {
                starredOnly && shouldMatchText -> "$COLUMN_STARRED=1 and $likeMatcher"
                !starredOnly && shouldMatchText -> likeMatcher
                !starredOnly && !shouldMatchText -> "$COLUMN_TITLE is not null AND $COLUMN_TITLE != ''"
                starredOnly && !shouldMatchText -> "$COLUMN_STARRED=1"

                else -> throw IllegalArgumentException()
            }
            val sorting = PostTable.getOrdinalSortingString()

            return Query(CONTENT_URI_POSTS, projection, selection, null, sorting)
        }

        fun threadHeaderQuery(id: Long): Query {
            val projection = PostTable.getFrontPageProjection()
            val selection = "${PostTable.COLUMN_ID}=?"
            val selectionArgs = arrayOf(id.toString())

            return Query(CONTENT_URI_POSTS, projection, selection, selectionArgs, null)
        }

        fun threadCommentsQuery(threadId: Long): Query {
            val projection = CommentsTable.getDefaultProjection()
            val selection = "${CommentsTable.COLUMN_PARENT}=?"
            val selectionArgs = arrayOf(threadId.toString())
            val sorting = CommentsTable.COLUMN_ORDINAL + " ASC"

            return Query(CONTENT_URI_COMMENTS, projection, selection, selectionArgs, sorting)
        }
    }
}

data class Query(val url: Uri, val projection: Array<String>, val selection: String, val selectionArgs: Array<String>?, val sorting: String?) {

    override fun hashCode(): Int {
        throw NotImplementedError()
    }

    override fun equals(other: Any?): Boolean {
        throw NotImplementedError()
    }
}
