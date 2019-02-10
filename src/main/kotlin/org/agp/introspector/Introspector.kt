package org.agp.introspector

import org.agp.introspector.providers.InformationSchemaProvider
import org.agp.introspector.providers.Provider
import javax.sql.DataSource

class Introspector(type: DatabaseType, dataSource: DataSource) {

    private val provider: Provider = initialize(type, dataSource)

    private fun initialize(type: DatabaseType, dataSource: DataSource): Provider {
        return if (type.supportInformationSchema) {
            InformationSchemaProvider(type, dataSource)
        } else {
            throw NotImplementedError("Supports only information schema-based databases.")
        }
    }
}
