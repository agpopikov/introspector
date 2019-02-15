package org.agp.introspector

import org.agp.introspector.providers.InformationSchemaProvider
import org.agp.introspector.providers.Provider
import javax.sql.DataSource

class Introspector(type: DatabaseType, dataSource: DataSource) {

    private val provider: Provider = initialize(type, dataSource)

    fun tables(schema: String) = provider.tables(schema)

    private fun initialize(type: DatabaseType, dataSource: DataSource): Provider {
        val informationSchemaDBs = setOf(DatabaseType.PostgreSQL, DatabaseType.MySQL, DatabaseType.MSSQL)
        return if (informationSchemaDBs.contains(type)) {
            InformationSchemaProvider(type, dataSource)
        } else {
            throw NotImplementedError("Supports only information schema-based databases.")
        }
    }
}
