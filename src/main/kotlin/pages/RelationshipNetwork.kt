package pages

import Vis
import VisData
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

interface DataItem
class Node(val id: Int, val label: String)
class Edge(val from: Int, val to: Int)
class Data(val nodes: dynamic, val edges: dynamic)
class Options

fun buildRelationshipNetwork() {
    val container = document.getElementById("relationship-network-section") as HTMLElement
    val nodes = (1..5).map { Node(it, "Node $it") }.toTypedArray()
    val edges = arrayOf(
        Edge(1, 3),
        Edge(1, 2),
        Edge(2, 4),
        Edge(2, 5),
        Edge(3, 3),
    )
    buildNetwork(container, nodes, edges)
}

private fun buildNetwork(container: HTMLElement, nodes: Array<Node>, edges: Array<Edge>) {
    val visData = VisData
    val visNet = Vis
    val data = Data(js("new visData.DataSet(nodes)"), js("new visData.DataSet(edges)"))
    val options = Options()
    js("new visNet.Network(container, data, options);")
}