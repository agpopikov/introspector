package org.agp.utils

import org.agp.introspector.Table
import java.io.File

class Visualizer {

    fun renderDotFile(tables: List<Table>, file: String) {
        var result = """
            digraph {
                graph [pad="0.5", nodesep="0.5", ranksep="2"];
                node [shape=plain]
                rankdir=LR;

        """.trimIndent()
        tables.forEach { t -> result += renderTableMarkup(t) }
        tables.forEach { t -> renderLinks(t)}
        result += "}"
        File(file).writeText(result)
    }

    private fun renderTableMarkup(table: Table): String {
        val items = mutableListOf("""<table border="0" cellborder="1" cellspacing="0">""")
        items.add("""<tr><td><i>${table.name}</i></td></tr>""")
        table.columns.forEach {
            val value = if (!it.nullable) "<b>${it.name}</b>" else it.name
            items.add("""<tr><td port="col-${it.name}">$value</td></tr>""")
        }
        items.add("""</table>""")
        return items.joinToString("\n")
    }

    private fun renderLinks(table: Table) {
        // TODO
    }
}
