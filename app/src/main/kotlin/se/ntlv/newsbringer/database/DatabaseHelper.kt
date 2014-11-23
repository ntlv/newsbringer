package se.ntlv.newsbringer.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


public class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DatabaseHelper.DATABASE_NAME, null, DatabaseHelper.DATABASE_VERSION) {

    // Method is called during creation of the database
    override fun onCreate(database: SQLiteDatabase) {
        PostTable.onCreate(database)
        CommentsTable.onCreate(database)
    }

    // Method is called during an upgrade of the database,
    // e.g. if you increase the database version
    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        PostTable.onUpgrade(database, oldVersion, newVersion)
        CommentsTable.onUpgrade(database, oldVersion, newVersion)
    }

    override fun onConfigure(database : SQLiteDatabase) {
        database.setForeignKeyConstraintsEnabled(true) //needed for cascading deletes

    }

    class object {
        private val DATABASE_NAME = "newsbringer.db"
        private val DATABASE_VERSION = 15
        private val LOG_TAG = "DatabaseHelper"
    }
}
