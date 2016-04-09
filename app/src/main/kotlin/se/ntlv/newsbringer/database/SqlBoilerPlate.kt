package se.ntlv.newsbringer.database

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

fun createTable(name : String, vararg fields : String) = "create table $name(${fields.joinToString()});"

infix fun String.int(qualifiers: String) = "$this integer $qualifiers"
infix fun String.text(qualifiers : String) = "$this text $qualifiers"
infix fun String.real(qualifiers : String) = "$this real $qualifiers"

fun cascadeDelete(table : String, column : String) = "REFERENCES $table($column) ON DELETE CASCADE"

val nullAllowed = ""
val notNull = "not null"
val defaultZero = "default 0"
val primaryKey = "primary key"

fun ContentResolver.query(uri : Uri, projection : Array<out String>, selection : String, selArgs : Array<out String>) : Cursor {
    return this.query(uri, projection, selection, selArgs, null)
}

fun ContentResolver.query(uri : Uri, projection : Array<out String>, selection : String) : Cursor =
        query(uri, projection, selection, null, null)

fun Cursor.getStringByName(columnName: String): String = getString(getColumnIndexOrThrow(columnName))
fun Cursor.getLongByName(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))
fun Cursor.getIntByName(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

val Cursor?.hasContent : Boolean
    get() = (this?.count ?: 0) > 0

fun <T> ContentResolver.getRowById(uri: Uri, projection: Array<String>, id: Long, f: (Cursor) -> T): T {
    val cursor = query(uri, projection, "_id=$id")
    cursor.moveToPositionOrThrow(0)
    val ret = f(cursor)
    cursor.close()
    return ret
}

fun ContentResolver.updateRowById(uri : Uri, id : Long, cv : ContentValues) {
    update(uri, cv, "_id=?", arrayOf(id.toString()))
}

fun <T> Cursor?.toList(extractor : ((Cursor) -> T)) : List<T> {
    if (this == null || hasContent.not()) return emptyList()
    moveToPositionOrThrow(0)
    val listInProgress = mutableListOf<T>()
    do {
        val value = extractor(this)
        listInProgress.add(value)
    } while (moveToNext())
    return emptyList<T>().plus(listInProgress)
}

fun Cursor.moveToPositionOrThrow(pos: Int) {
    if (this.isClosed || moveToPosition(pos).not()) {
        throw IndexOutOfBoundsException()
    }
}