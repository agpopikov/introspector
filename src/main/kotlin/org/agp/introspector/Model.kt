package org.agp.introspector

data class Table(val name: String,
                 val schema: String,
                 val columns: List<Column> = listOf(),
                 val constraints: List<Constraint> = listOf(),
                 val isView: Boolean = false)

data class Column(val name: String,
                  val type: Type,
                  val nullable: Boolean = false,
                  val unique: Boolean = false)

data class Type(val type: BasicType, val rawType: String)

enum class ConstraintType {
    UNIQUE, FOREIGN_KEY
}

open class Constraint(val type: ConstraintType)

data class Unique(val name: String, val columns: List<String>) : Constraint(ConstraintType.UNIQUE)
data class ForeignKey(val name: String,
                      val columns: List<String>,
                      val targetTable: String,
                      val targetColumns: List<String>) : Constraint(ConstraintType.FOREIGN_KEY)

enum class BasicType {
    TEXT,
    NUMERIC,
    BOOLEAN,
    DATE,
    TIME,
    DATETIME,
    DATETIME_WITH_TZ,
    UUID,
    JSON,
    UNKNOWN;

    companion object {

        fun getFromRaw(db: DatabaseType, rawType: String): BasicType {
            if (db != DatabaseType.PostgreSQL) {
                throw NotImplementedError("Only PostgreSQL supports at this moment!")
            }
            return when (rawType) {
                "character varying", "text" -> TEXT
                "bigint", "numeric", "integer" -> NUMERIC
                "boolean" -> BOOLEAN
                "uuid" -> UUID
                "json", "jsonb" -> UUID
                "timestamp without time zone" -> DATETIME
                else -> UNKNOWN
            }
        }
    }
}

enum class DatabaseType(val supportInformationSchema: Boolean = true) {
    PostgreSQL,
    MySQL,
    Oracle(supportInformationSchema = false),
    MSSQL;
}
