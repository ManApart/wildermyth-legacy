package pages

import Data
import Edge
import Node
import Options
import Vis
import VisData
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

fun buildRelationshipNetwork() {
    val container = document.getElementById("relationship-network-section") as HTMLElement
    val visData = VisData
    val visNet = Vis
    val data = Data(js("new visData.DataSet([\n" +
            "        { id: 1, label: \"Node 1\" },\n" +
            "        { id: 2, label: \"Node 2\" },\n" +
            "        { id: 3, label: \"Node 3\" },\n" +
            "        { id: 4, label: \"Node 4\" },\n" +
            "        { id: 5, label: \"Node 5\" },\n" +
            "      ])"), js("new visData.DataSet([\n" +
            "        { from: 1, to: 3 },\n" +
            "        { from: 1, to: 2 },\n" +
            "        { from: 2, to: 4 },\n" +
            "        { from: 2, to: 5 },\n" +
            "        { from: 3, to: 3 },\n" +
            "      ])"))
    val options = Options()
    js("new visNet.Network(container, data, options);")
}