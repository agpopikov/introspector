package org.agp.introspector.providers

import org.agp.introspector.DatabaseType
import org.agp.introspector.Table
import javax.sql.DataSource

/**
 * TODO
 */
class OracleProvider(dataSource: DataSource): Provider(DatabaseType.Oracle, dataSource) {

    override fun tables(schema: String, filter: String?): List<Table> {
        TODO("not implemented")
    }
}
