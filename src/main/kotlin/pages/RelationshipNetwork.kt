package pages

import Data
import DataSet
import Edge
import Node
import Vis
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

fun buildRelationshipNetwork() {
    val container = document.getElementById("relationship-network-section") as HTMLElement
    val node = Node(1, "Node 1")
    val data = Data(DataSet(
        (1..5).map { Node(it, "Node $it") }, null),
        DataSet(
            listOf(
                Edge(1, 3),
                Edge(1, 2),
                Edge(2, 4),
                Edge(2, 5),
                Edge(3, 3),
            ), null
        ))
    Vis.Network(container, data, null)

}