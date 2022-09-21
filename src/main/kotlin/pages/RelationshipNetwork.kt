package pages

import Data
import Data2
import Edge
import Node
import Options
import Vis
import VisData
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
    val ds = (1..6).map { Node(it, "Node $it") }.toTypedArray()
    val ds2 = arrayOf(
        Edge(1, 3),
        Edge(1, 2),
        Edge(2, 4),
        Edge(2, 5),
        Edge(3, 3),
    )
    val vis = VisData
    val visNet = Vis
    val data2 = Data2(js("new vis.DataSet([\n" +
            "        { id: 1, label: \"Node 1\" },\n" +
            "        { id: 2, label: \"Node 2\" },\n" +
            "        { id: 3, label: \"Node 3\" },\n" +
            "        { id: 4, label: \"Node 4\" },\n" +
            "        { id: 5, label: \"Node 5\" },\n" +
            "      ])"), js("new vis.DataSet([\n" +
            "        { from: 1, to: 3 },\n" +
            "        { from: 1, to: 2 },\n" +
            "        { from: 2, to: 4 },\n" +
            "        { from: 2, to: 5 },\n" +
            "        { from: 3, to: 3 },\n" +
            "      ])"))
    println(JSON.stringify(data2))
//    Vis.Network(container, data2, null)
    val options = Options()
    js("new visNet.Network(container, data2, options);")
}