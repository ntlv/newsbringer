package se.ntlv.newsbringer.database

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import android.util.Log

public class NewsContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        database = DatabaseHelper(getContext())
        Log.i(LOG_TAG, "Content provider created")
        return true
    }

    // database
    private var database: DatabaseHelper? = null

    override fun query(uri: Uri, projection: Array<String>, selection: String?, selectionArguments: Array<String>?, sortOrder: String?): Cursor {

        val queryBuilder = SQLiteQueryBuilder()
        when (sUriMatcher.match(uri)) {
            POST_ID -> {
                queryBuilder.appendWhere(PostTable.COLUMN_ID + "=" + uri.getLastPathSegment())
                queryBuilder.setTables(PostTable.TABLE_NAME)
            }
            POSTS -> queryBuilder.setTables(PostTable.TABLE_NAME)
        }//case SOME_OTHER_CONTENT
        //no dice, don't do anything

        val db = database!!.getReadableDatabase()
        val cursor = queryBuilder.query(db, projection, selection, selectionArguments, null, null, sortOrder)

        cursor.setNotificationUri(getContext().getContentResolver(), uri)

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues): Uri {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database!!.getWritableDatabase()

        when {
            uriType == POSTS -> {
                val id = sqlDB.insert(PostTable.TABLE_NAME, null, contentValues)
                getContext().getContentResolver().notifyChange(uri, null)
                return Uri.parse(CONTENT_1 + "/" + id)
            }
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
    }

    override fun delete(uri: Uri, selection: String?, selectionArguments: Array<String>?): Int {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database!!.getWritableDatabase()
        var rowsDeleted : Int

        when (uriType) {
            POSTS -> rowsDeleted = sqlDB.delete(PostTable.TABLE_NAME, selection, selectionArguments)
            POST_ID -> {
                val id = uri.getLastPathSegment()
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(PostTable.TABLE_NAME, PostTable.COLUMN_ID + "=" + id, null)
                } else {
                    rowsDeleted = sqlDB.delete(PostTable.TABLE_NAME, PostTable.COLUMN_ID + "=" + id + " and " + selection, selectionArguments)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        getContext().getContentResolver().notifyChange(uri, null)
        return rowsDeleted
    }

    override fun update(uri: Uri, values: ContentValues, selection: String, selectionArgs: Array<String>?): Int {
        val uriType = sUriMatcher.match(uri)
        val sqlDB = database!!.getWritableDatabase()
        var rowsUpdated : Int
        when (uriType) {
            POSTS -> rowsUpdated = sqlDB.update(PostTable.TABLE_NAME, values, selection, selectionArgs)
            POST_ID -> {
                val id = uri.getLastPathSegment()
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(PostTable.TABLE_NAME, values, PostTable.COLUMN_ID + "=" + id, null)
                } else {
                    rowsUpdated = sqlDB.update(PostTable.TABLE_NAME, values, PostTable.COLUMN_ID + "=" + id + " and " + selection, selectionArgs)
                }
            }
            else -> throw IllegalArgumentException("Unknown URI: " + uri)
        }
        getContext().getContentResolver().notifyChange(uri, null)
        return rowsUpdated
    }

    class object {

        private val POSTS = 10
        private val POST_ID = 20

        private val AUTHORITY = "se.ntlv.newsbringer.database.NewsContentProvider"

        private val CONTENT_1 = "posts"
        public val CONTENT_URI: Uri = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_1)


        public val CONTENT_TYPE: String = ContentResolver.CURSOR_DIR_BASE_TYPE + "/posts"
        public val CONTENT_ITEM_TYPE: String = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/post"

        public var sUriMatcher: UriMatcher

        private val LOG_TAG = "ScheduleContentProvider"

        {
            sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
            sUriMatcher.addURI(AUTHORITY, CONTENT_1, POSTS)
            sUriMatcher.addURI(AUTHORITY, CONTENT_1 + "/#", POST_ID)
        }
    }
}
