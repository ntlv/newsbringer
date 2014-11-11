package se.ntlv.newsbringer.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper {

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	private static String DATABASE_NAME = "newsbringer.db";
	private static int  DATABASE_VERSION = 6;
	private static String LOG_TAG = "DatabaseHelper";

	@Override
	public void onCreate(SQLiteDatabase db) {
		PostTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		PostTable.onUpgrade(db, oldVersion, newVersion);
	}
}

