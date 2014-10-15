package se.ntlv.newsbringer.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PostTable {

	// Database table
	public static final String TABLE_NAME = "posts";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_ORDINAL = "ordinal";
	public static final String COLUMN_TIMESTAMP = "timestamp";


	// Database creation SQL statement
	private static final String DATABASE_CREATE = "create table " + TABLE_NAME
			+ "("
			+ COLUMN_ID + " integer primary key, "
			+ COLUMN_ORDINAL + " integer not null, "
			+ COLUMN_TIMESTAMP + " text not null "
			//+ " FOREIGN KEY (" + COLUMN_ID + ") REFERENCES "
			//+ ITAuthorTable.TABLE_NAME + " (" + ITAuthorTable.COLUMN_ID + ")"
			+ ");";

	private static final String LOG_TAG = "PostTable";

	public static void onCreate(SQLiteDatabase database) {
		Log.d(LOG_TAG, "creating database");
		database.execSQL(DATABASE_CREATE);

	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
								 int newVersion) {
		Log.w(LOG_TAG,
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(database);
	}
}



