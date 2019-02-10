package org.agp.utils

import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource


class DB(private val ds: DataSource) {

    private val log = LoggerFactory.getLogger(DB::class.java)

    /**
     * Returns single element from DB using specified query and params, and null if ResultSet is empty.
     */
    @JvmOverloads
    fun <T> querySingle(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf(), mapper: (ResultSet) -> T): T? {
        var result: T? = null
        executeStatement(query, params) {
            val rs = it.executeQuery()
            if (rs.next()) {
                result = mapper(rs)
            }
        }
        return result
    }

    /**
     * Returns single element (with type - Long) using specified [query] and [params].
     * Can be handy when trying to select count of elements or any sort of id.
     */
    @JvmOverloads
    fun queryLong(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf()): Long? {
        return querySingle(query, params) { it.getLong(1) }
    }

    /**
     * Returns single element (with type - Boolean) using specified [query] and [params].
     * Can be handy when trying to evaluate some condition in SQL (example - record exists or count > 10).
     */
    @JvmOverloads
    fun queryBoolean(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf()): Boolean? {
        return querySingle(query, params) { it.getBoolean(1) }
    }

    /**
     * Returns single element (with type - String) using specified [query] and [params].
     */
    @JvmOverloads
    fun queryString(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf()): String? {
        return querySingle(query, params) { it.getString(1) }
    }

    /**
     * Returns list of element from DB using specified [query] and [params].
     * Output results can be mapped using [mapper] function.
     *
     * Optionally can be set [fetchSize] parameters (accords directly to JDBC fetch size).
     * @return list of mapped items.
     */
    @JvmOverloads
    fun <T> queryMany(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf(),
                      fetchSize: Int = 1000, mapper: (ResultSet) -> T): List<T> {
        val result = mutableListOf<T>()
        executeStatement(query, params) {
            val rs = it.executeQuery()
            rs.fetchSize = fetchSize
            while (rs.next()) {
                result.add(mapper(rs))
            }
        }
        return result
    }

    @JvmOverloads
    fun execute(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf()) {
        executeStatement(query, params) {
            it.executeUpdate()
        }
    }

    @JvmOverloads
    fun executeReturningLongKey(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf()): Long? {
        var result: Long? = null
        executeStatement(query, params, returningKey = true) {
            it.executeUpdate()
            val keys = it.generatedKeys
            if (keys.next()) {
                result = keys.getLong(1)
            }
        }
        return result
    }

    @JvmOverloads
    fun <T> executeReturningKey(@Language("GenericSQL") query: String, params: Map<String, Any> = mapOf(), mapper: (ResultSet) -> T?): T? {
        var result: T? = null
        executeStatement(query, params, returningKey = true) {
            it.executeUpdate()
            val keys = it.generatedKeys
            if (keys.next()) {
                result = mapper(keys)
            }
        }
        return result
    }

    private inline fun executeStatement(query: String, params: Map<String, Any>, returningKey: Boolean = false,
                                        run: (PreparedStatement) -> Unit) {
        var connection: Connection? = null
        var statement: PreparedStatement? = null
        try {
            connection = ds.connection
            try {
                val (rawQuery, rawParams) = transform(query, params)
                if (returningKey) {
                    statement = connection.prepareStatement(rawQuery, Statement.RETURN_GENERATED_KEYS)
                } else {
                    statement = connection.prepareStatement(rawQuery)
                }
                rawParams.forEachIndexed { i, value -> statement.setObject(i + 1, value) }
                run(statement)
            } catch (e: Exception) {
                log.error("Failed to execute: ${e.message}", e)
            } finally {
                statement?.close()
            }
        } catch (e: Exception) {
            log.error("Failed to execute: ${e.message}", e)
        } finally {
            connection?.close()
        }
    }

    /**
     * Converts named parameter query (that uses :example syntax for defining SQL parameters)
     * to simple parameter query (that uses ? sign), because JDBC doesn't support out of the box
     * named parameter queries.
     */
    private fun transform(query: String, params: Map<String, Any>): Pair<String, List<Any>> {
        var inSingleQuote = false
        var inDoubleQuote = false
        val indexedQuery = StringBuffer(query.length)
        val indexedParams = mutableListOf<Any>()

        var i = 0
        while (i < query.length) {
            var c = query[i]
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true
                } else if (c == '"') {
                    inDoubleQuote = true
                } else if (c == ':' && i + 1 < query.length && Character.isJavaIdentifierStart(query[i + 1])) {
                    var j = i + 2
                    while (j < query.length && Character.isJavaIdentifierPart(query[j])) {
                        j++
                    }
                    val name = query.substring(i + 1, j)
                    c = '?'
                    i += name.length
                    val param = params[name]
                    if (param != null) {
                        indexedParams.add(param)
                    } else {
                        log.warn("Not passed parameter $name in query: $query")
                    }
                }
            }
            indexedQuery.append(c)
            i++
        }
        return Pair(indexedQuery.toString(), indexedParams)
    }
}
