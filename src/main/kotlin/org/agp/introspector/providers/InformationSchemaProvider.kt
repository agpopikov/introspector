package org.agp.introspector.providers

import org.agp.introspector.*
import org.agp.utils.DB
import javax.sql.DataSource

class InformationSchemaProvider(private val type: DatabaseType, dataSource: DataSource) : Provider(type, dataSource) {

    private val db = DB(dataSource)

    override fun tables(schema: String, filter: String?): List<Table> {
        // get tables
        val tables = getTables(schema)
        TODO()
    }

    private fun getTables(schema: String): List<Table> {
        val query = """
            SELECT table_name, table_type
            FROM information_schema.tables
            WHERE table_schema like :schema
        """
        return db.queryMany(query, mapOf("schema" to schema)) {
            val table = it.getString("table_name")
            val isView = it.getString("table_type") != "BASE TABLE"
            val constraints = mutableListOf<Constraint>()
            constraints.addAll(getUniqueConstraints(schema, table))
            constraints.addAll(getForeignKeyConstraints(schema, table))
            Table(schema, table, isView = isView, columns = getColumns(schema, table))
        }
    }

    private fun getColumns(schema: String, table: String): List<Column> {
        val query = """
            SELECT column_name, is_nullable, data_type
            FROM information_schema.columns
            WHERE table_schema LIKE :schema AND table_name LIKE :table
            ORDER BY ordinal_position;
        """
        return db.queryMany(query, mapOf("schema" to schema, "table" to table)) {
            val name = it.getString("column_name")
            val raw = it.getString("data_type")
            val nullable = it.getBoolean("is_nullable")
            Column(name, Type(BasicType.getFromRaw(type, raw), raw), nullable)
        }
    }

    private fun getForeignKeyConstraints(schema: String, table: String): List<ForeignKey> {
        val query = """
            SELECT
              s.constraint_name AS name,
              s.column_name AS source_column,
              t.table_name AS target_table,
              t.column_name AS target_column
            FROM information_schema.referential_constraints AS c
                   INNER JOIN information_schema.key_column_usage AS s
            ON s.constraint_catalog = c.constraint_catalog
              AND s.constraint_schema = c.constraint_schema
              AND s.constraint_name = c.constraint_name
                   INNER JOIN information_schema.key_column_usage AS t
            ON t.constraint_catalog = c.unique_constraint_catalog
              AND t.constraint_schema = c.unique_constraint_schema
              AND t.constraint_name = c.unique_constraint_name
              AND t.ordinal_position = s.ordinal_position
            WHERE s.table_schema LIKE :schema AND s.table_name LIKE :table
        """
        val items = mutableMapOf<String, ForeignKey>()
        db.queryMany(query, mapOf("schema" to schema, "table" to table)) {
            val name = it.getString("name")
            val sourceColumn = it.getString("source_column")
            val targetTable = it.getString("target_table")
            val targetColumn = it.getString("target_column")
            val existing = items[name]
            if (existing != null) {
                items[name] = existing.copy(columns = existing.columns.concat(sourceColumn), targetColumns = existing.targetColumns.concat(targetColumn))
            } else {
                items[name] = ForeignKey(name, listOf(sourceColumn), targetTable, listOf(targetColumn))
            }
        }
        return items.values.toList()
    }

    private fun getUniqueConstraints(schema: String, table: String): List<Unique> {
        val query = """
            SELECT tc.constraint_name as name, ccu.column_name as column
            FROM information_schema.table_constraints tc
                   JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
            WHERE tc.constraint_type = 'UNIQUE' AND tc.table_schema LIKE :schema AND tc.table_name LIKE :table

        """
        val items = mutableMapOf<String, Unique>()
        db.queryMany(query, mapOf("schema" to schema, "table" to table)) {
            val name = it.getString("name")
            val column = it.getString("column")
            val existing = items[name]
            if (existing != null) {
                items[name] = existing.copy(columns = existing.columns.concat(column))
            } else {
                items[name] = Unique(name, listOf(column))
            }
        }
        return items.values.toList()
    }

    private fun <T> List<T>.concat(item: T): List<T> {
        val result = this.toMutableList()
        result.add(item)
        return result.toList()
    }
}
