package se.ntlv.newsbringer.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PostTable {

		// Database table
		public static String TABLE_NAME = "posts";

		public static String COLUMN_ID = "_id";
		public static String COLUMN_SCORE = "score";
		public static String COLUMN_TIMESTAMP = "timestamp";
		public static String COLUMN_BY = "by";
		public static String COLUMN_CHILDREN = "children";
		public static String COLUMN_TEXT = "text";
		public static String COLUMN_TITLE = "title";
		public static String COLUMN_TYPE = "type";
		public static String COLUMN_URL = "url";
		public static String COLUMN_ORDINAL = "ordinal";

		// Database creation SQL statement
		private static String DATABASE_CREATE = "create table " + TABLE_NAME +
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
				COLUMN_ORDINAL + " real not null " +
				//+ " FOREIGN KEY (" + COLUMN_ID + ") REFERENCES "
				//+ ITAuthorTable.TABLE_NAME + " (" + ITAuthorTable.COLUMN_ID + ")"
				");";

		private static String LOG_TAG = "PostTable";

		public static void onCreate(SQLiteDatabase database) {
			Log.d(LOG_TAG, "creating database");
			database.execSQL(DATABASE_CREATE);

		}

		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(database);
		}

}




