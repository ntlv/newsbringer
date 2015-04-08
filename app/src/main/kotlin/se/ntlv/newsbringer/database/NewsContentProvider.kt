package se.ntlv.newsbringer.database

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import kotlin.properties.Delegates

public class NewsContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        Log.i(LOG_TAG, "Content provider created")
        return true
    }

    val database: DatabaseHelper by Delegates.lazy {
        DatabaseHelper(getContext())
    }


    override fun query(uri: Uri, projection: Array<String>, selection: String?, selectionArguments: Array<String>?, sortOrder: String?): Cursor {

        val queryBuilder = SQLiteQueryBuilder()
        when (sUriMatcher.match(uri)) {
            POST_ID -> {
                queryBuilder.appendWhere(PostTable.COLUMN_ID + "=" + uri.getLastPathSegment())
                queryBuilder.setTables(PostTable.TABLE_NAME)
            }
            COMMENTS_ID -> {
                queryBuilder.appendWhere(CommentsTable.COLUMN_ID + "=" + uri.getLastPathSegment())
                queryBuilder.setTables(CommentsTable.TABLE_NAME)
            }
            POSTS -> queryBuilder.setTables(PostTable.TABLE_NAME)
            COMMENTS -> queryBuilder.setTables(CommentsTable.TABLE_NAME)
        }

        val db = database.getReadableDatabase()
        val cursor = queryBuilder.query(db, projection, selection, selectionArguments, null, null, sortOrder)

        cursor.setNotificationUri(getContext().getContentResolver(), uri)

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues): Uri {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database.getWritableDatabase()

        when (uriType) {
            POSTS -> {
                var id = sqlDB.insert(PostTable.TABLE_NAME, null, contentValues)
                if (id == -1L) {
                    id = sqlDB.update(PostTable.TABLE_NAME, contentValues, "${PostTable.COLUMN_ID}=?",
                            array(contentValues.getAsLong(PostTable.COLUMN_ID).toString())
                    ).toLong()
                }
                getContext().getContentResolver().notifyChange(uri, null)
                return Uri.parse(CONTENT_POSTS + "/" + id)
            }
            COMMENTS -> {
                val id = sqlDB.replace(CommentsTable.TABLE_NAME, null, contentValues)
                getContext().getContentResolver().notifyChange(uri, null)
                return Uri.parse(CONTENT_COMMENTS + "/" + id)
            }
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArguments: Array<String>?): Int {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database.getWritableDatabase()


        val rowsDeleted = when (uriType) {
            POSTS -> sqlDB.delete(PostTable.TABLE_NAME, selection, selectionArguments)
            POST_ID -> {
                val id = uri.getLastPathSegment()
                if (TextUtils.isEmpty(selection)) {
                    sqlDB.delete(PostTable.TABLE_NAME, PostTable.COLUMN_ID + "=" + id, null)
                } else {
                    sqlDB.delete(PostTable.TABLE_NAME, PostTable.COLUMN_ID + "=" + id + " and " + selection, selectionArguments)
                }
            }
            COMMENTS -> sqlDB.delete(CommentsTable.TABLE_NAME, selection, selectionArguments)
            POST_ID -> {
                val id = uri.getLastPathSegment()
                if (TextUtils.isEmpty(selection)) {
                    sqlDB.delete(CommentsTable.TABLE_NAME, CommentsTable.COLUMN_ID + "=" + id, null)
                } else {
                    sqlDB.delete(CommentsTable.TABLE_NAME, CommentsTable.COLUMN_ID + "=" + id + " and " + selection, selectionArguments)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        getContext().getContentResolver().notifyChange(uri, null)
        return rowsDeleted
    }

    override fun update(uri: Uri, values: ContentValues, selection: String, selectionArgs: Array<String>?): Int {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database.getWritableDatabase()
        val rowsUpdated = when (uriType) {
            POSTS -> sqlDB.update(PostTable.TABLE_NAME, values, selection, selectionArgs)
            COMMENTS -> sqlDB.update(CommentsTable.TABLE_NAME, values, selection, selectionArgs)
            POST_ID -> handleItemUpdate(selection, selectionArgs, sqlDB, values, PostTable.TABLE_NAME, PostTable.COLUMN_ID, uri.getLastPathSegment())
            COMMENTS_ID -> handleItemUpdate(selection, selectionArgs, sqlDB, values, CommentsTable.TABLE_NAME, CommentsTable.COLUMN_ID, uri.getLastPathSegment())
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        getContext().getContentResolver().notifyChange(uri, null)
        return rowsUpdated ?: 0
    }

    private fun handleItemUpdate(selection: String,
                                 selectionArgs: Array<String>?,
                                 sqlDB: SQLiteDatabase?,
                                 values: ContentValues,
                                 tableName: String,
                                 idColumn: String,
                                 itemId: String): Int? {
        return when {
            TextUtils.isEmpty(selection) -> sqlDB?.update(tableName, values, idColumn + "=" + itemId, null)
            else -> sqlDB?.update(tableName, values, idColumn + "=" + itemId + " and " + selection, selectionArgs)
        }
    }

    companion object {

        private val POSTS = 10
        private val POST_ID = 20

        private val COMMENTS = 30
        private val COMMENTS_ID = 40

        private val AUTHORITY = "se.ntlv.newsbringer.database.NewsContentProvider"

        private val CONTENT_POSTS = "posts"
        public val CONTENT_URI_POSTS: Uri = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_POSTS)

        private val CONTENT_COMMENTS = "comments"

        public val CONTENT_URI_COMMENTS: Uri = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_COMMENTS)


        public val CONTENT_TYPE: String = ContentResolver.CURSOR_DIR_BASE_TYPE + "/posts"
        public val CONTENT_ITEM_TYPE: String = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/post"

        public var sUriMatcher: UriMatcher

        private val LOG_TAG = "NewsContentProvider"

        init {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher.addURI(AUTHORITY, CONTENT_POSTS, POSTS)
            sUriMatcher.addURI(AUTHORITY, "$CONTENT_POSTS/#", POST_ID)
            sUriMatcher.addURI(AUTHORITY, CONTENT_COMMENTS, COMMENTS)
            sUriMatcher.addURI(AUTHORITY, "$CONTENT_COMMENTS/#", COMMENTS_ID)
        }
    }
}
