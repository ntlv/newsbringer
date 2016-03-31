package se.ntlv.newsbringer.database

fun createTable(name : String, vararg fields : String) = "create table $name(${fields.joinToString()});"

infix fun String.int(qualifiers: String) = "$this integer $qualifiers"
infix fun String.text(qualifiers : String) = "$this text $qualifiers"
infix fun String.real(qualifiers : String) = "$this real $qualifiers"

fun cascadeDelete(table : String, column : String) = "REFERENCES $table($column) ON DELETE CASCADE"

val notNull = "not null"
val defaultZero = "default 0"
val primaryKey = "primary key"