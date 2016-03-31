package se.ntlv.newsbringer.database

import android.database.Cursor

fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))
fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))
