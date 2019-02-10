package org.agp.introspector.providers

import org.agp.introspector.*
import org.agp.utils.DB
import javax.sql.DataSource

class InformationSchemaProvider(private val type: DatabaseType, dataSource: DataSource) : Provider(type, dataSource) {

    private val db = DB(dataSource)

    override fun tables(schema: String, filter: String?): List<Table> {
        // get tables
        val tablesQuery = """
            SELECT table_name, table_type
            FROM information_schema.tables
            WHERE table_schema like :schema
        """
        val tables = db.queryMany(tablesQuery, mapOf("schema" to schema)) {
            val name = it.getString("table_name")
            val isView = it.getString("table_type") != "BASE TABLE"
            Table(schema, name, isView = isView)
        }
        // get columns for table
        val columnsQuery = """
            SELECT column_name, is_nullable, data_type
            FROM information_schema.columns
            WHERE table_schema LIKE :schema AND table_name LIKE :table
            ORDER BY ordinal_position;
        """
        val columns = db.queryMany(columnsQuery, mapOf("schema" to schema, "table" to tables.first().name)) {
            val name = it.getString("column_name")
            val raw = it.getString("data_type")
            val nullable = it.getBoolean("is_nullable")
            Column(name, Type(BasicType.getFromRaw(type, raw), raw), nullable)
        }
        // get referential constraints for table
        val referentialConstraintsQuery = """
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
        val fks = mutableMapOf<String, ForeignKey>()
        db.queryMany(referentialConstraintsQuery, mapOf("schema" to schema, "table" to tables.first().name)) {
            val name = it.getString("name")
            val sourceColumn = it.getString("source_column")
            val targetTable = it.getString("target_table")
            val targetColumn = it.getString("target_column")
            val existing = fks[name]
            if (existing != null) {
                fks[name] = existing.copy(columns = existing.columns.concat(sourceColumn), targetColumns = existing.targetColumns.concat(targetColumn))
            } else {
                fks[name] = ForeignKey(name, listOf(sourceColumn), targetTable, listOf(targetColumn))
            }
            Unit
        }
        //
        TODO()
    }

    private fun <T> List<T>.concat(item: T): List<T> {
        val result = this.toMutableList()
        result.add(item)
        return result.toList()
    }
}
