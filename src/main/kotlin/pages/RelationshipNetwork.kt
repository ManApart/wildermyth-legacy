package pages

import Data
import Edge
import Node
import Options
import Vis
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

fun buildRelationshipNetwork() {
    val container = document.getElementById("relationship-network-section") as HTMLElement
    val data = Data(
        VisData.DataSet(
            (1..6).map { Node(it, "Node $it") }.toTypedArray(), null
        ),
        VisData.DataSet(
            arrayOf(
                Edge(1, 3),
                Edge(1, 2),
                Edge(2, 4),
                Edge(2, 5),
                Edge(3, 3),
            ), null
        )
    )
    Vis.Network(container, data, null)

}