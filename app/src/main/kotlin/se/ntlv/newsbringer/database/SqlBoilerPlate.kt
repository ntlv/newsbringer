package se.ntlv.newsbringer.database

import android.content.ContentValues
import android.database.Cursor

fun createTable(name: String, vararg fields: String) = "create table $name(${fields.joinToString()});"

infix fun String.int(qualifiers: String) = "$this integer $qualifiers"
infix fun String.text(qualifiers: String) = "$this text $qualifiers"
infix fun String.real(qualifiers: String) = "$this real $qualifiers"

fun cascadeDelete(table: String, column: String) = "REFERENCES $table($column) ON DELETE CASCADE"

val nullAllowed = ""
val notNull = "not null"
val defaultZero = "default 0"
val primaryKey = "primary key"

fun Cursor.getStringByName(columnName: String): String = getString(getColumnIndexOrThrow(columnName)) ?: throw NullPointerException("$columnName on row $position was null")

fun Cursor.getLongByName(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))
fun Cursor.getIntByName(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

fun <T> Cursor.map(transform: (Cursor) -> T): List<T> {
    if (isClosed) {
        throw IllegalAccessException("Cannot map on closed cursor.")
    }
    return (0..count - 1).map {
        if (!moveToPosition(it)) {
            throw IndexOutOfBoundsException("Cannot move to $it with $count items.")
        }
        transform(this)
    }
}

fun contentValuesOf(vararg pairs: Pair<String, Any>): ContentValues {
    val cv = ContentValues(pairs.size)
    pairs.forEach {
        val rowValue = it.second
        when (rowValue) {
            is Byte -> cv.put(it.first, rowValue)
            is Short -> cv.put(it.first, rowValue)
            is Int -> cv.put(it.first, rowValue)
            is Long -> cv.put(it.first, rowValue)
            is Float -> cv.put(it.first, rowValue)
            is Double -> cv.put(it.first, rowValue)
            is Boolean -> cv.put(it.first, rowValue)
            is ByteArray -> cv.put(it.first, rowValue)
            is String -> cv.put(it.first, rowValue)
            else -> IllegalArgumentException("Bad type for convent values value")
        }
    }
    return cv
}
