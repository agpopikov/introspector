package org.agp.introspector.providers

import org.agp.introspector.DatabaseType
import org.agp.introspector.Table
import javax.sql.DataSource

/**
 *
 */
abstract class Provider(type: DatabaseType, dataSource: DataSource) {

    abstract fun tables(schema: String, filter: String? = null): List<Table>
}
