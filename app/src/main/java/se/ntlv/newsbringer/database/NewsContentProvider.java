package se.ntlv.newsbringer.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class NewsContentProvider extends ContentProvider {
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/posts";
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/post";
	public static final UriMatcher sUriMatcher;
	private static final int POSTS = 10;
	private static final int POST_ID = 20;
	private static final String AUTHORITY = "se.ntlv.newsbringer.database.NewsContentProvider";
	private static final String CONTENT_1 = "posts";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTENT_1);
	private static final String LOG_TAG = "ScheduleContentProvider";

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, CONTENT_1, POSTS);
		sUriMatcher.addURI(AUTHORITY, CONTENT_1 + "/#", POST_ID);
	}

	// database
	private DatabaseHelper database;

	@Override
	public boolean onCreate() {
		database = new DatabaseHelper(getContext());
		Log.i(LOG_TAG, "Content provider created");
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArguments, String sortOrder) {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		switch (sUriMatcher.match(uri)) {
			case POST_ID:
				queryBuilder.appendWhere(PostTable.COLUMN_ID + "="
						+ uri.getLastPathSegment());
			case POSTS:
				queryBuilder.setTables(PostTable.TABLE_NAME);
				break;
			//case SOME_OTHER_CONTENT
			default:
				//no dice, don't do anything
		}

		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArguments, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues contentValues) {
		int uriType = sUriMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();

		long id = 0;
		switch (uriType) {
			case POSTS:
				id = sqlDB.insert(PostTable.TABLE_NAME, null, contentValues);
				getContext().getContentResolver().notifyChange(uri, null);
				return Uri.parse(CONTENT_1 + "/" + id);
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArguments) {
		int uriType = sUriMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
			case POSTS:
				rowsDeleted = sqlDB.delete(PostTable.TABLE_NAME, selection,
						selectionArguments);
				break;
			case POST_ID:
				String id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsDeleted = sqlDB.delete(PostTable.TABLE_NAME,
							PostTable.COLUMN_ID + "=" + id, null);
				} else {
					rowsDeleted = sqlDB.delete(PostTable.TABLE_NAME,
							PostTable.COLUMN_ID + "=" + id + " and " + selection,
							selectionArguments);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		int uriType = sUriMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
			case POSTS:
				rowsUpdated = sqlDB.update(PostTable.TABLE_NAME, values, selection,
						selectionArgs);
				break;
			case POST_ID:
				String id = uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					rowsUpdated = sqlDB.update(PostTable.TABLE_NAME, values,
							PostTable.COLUMN_ID + "=" + id, null);
				} else {
					rowsUpdated = sqlDB.update(PostTable.TABLE_NAME, values,
							PostTable.COLUMN_ID + "=" + id + " and " + selection,
							selectionArgs);
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}
}
