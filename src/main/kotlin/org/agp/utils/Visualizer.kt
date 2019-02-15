package org.agp.utils

import org.agp.introspector.ForeignKey
import org.agp.introspector.Table
import java.io.File

object Visualizer {

    fun renderDotFile(tables: List<Table>, file: String) {
        val tablesDefs = tables.joinToString("\n") { t -> renderTableMarkup(t) }
        val linksDefs = tables.joinToString("\n") { t -> renderLinks(t) }
        val result = """
            digraph {
                graph [pad="0.5", nodesep="0.5", ranksep="2"];
                node [shape=plain]
                rankdir=LR;

            $tablesDefs
            $linksDefs
            }
        """.trimIndent()
        File(file).writeText(result)
    }

    private fun renderTableMarkup(table: Table): String {
        val items = mutableListOf("""<table border="0" cellborder="1" cellspacing="0">""")
        items.add("""<tr><td><i>${table.name}</i></td></tr>""")
        table.columns.forEach {
            val value = if (!it.nullable) "<b>${it.name}</b>" else it.name
            items.add("""<tr><td port="${it.name}">$value</td></tr>""")
        }
        items.add("""</table>""")
        return """${table.name} [label=<${items.joinToString("\n")}>];"""
    }

    private fun renderLinks(table: Table): String {
        val items = mutableListOf<String>()
        table.constraints.forEach {
            if (it is ForeignKey) {
                val source = it.columns.first()
                val target = it.targetColumns.first()
                items.add("""${table.name}:$source -> ${it.targetTable}:$target [label="${it.name}"];""")
            }
        }
        return items.joinToString("\n")
    }
}
