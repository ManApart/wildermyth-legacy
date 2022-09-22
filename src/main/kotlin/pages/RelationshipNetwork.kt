package pages

import LegacyCharacter
import Vis
import VisData
import clearSections
import el
import getCharacter
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.button
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement

interface DataItem
class Node(val id: Int, val label: String)
class Edge(val from: Int, val to: Int)
class Data(val nodes: dynamic, val edges: dynamic)
class Options

fun buildRelationshipNetwork(character: LegacyCharacter) {
    val snapshot = character.snapshots.last()
    clearSections()
    document.title = snapshot.name
    document.documentElement?.scrollTop = 0.0
    window.history.pushState(null, "null", "#network/" + character.uuid)
    setFavicon(character)
    buildPage(character)
    buildNetwork(character)
}

private fun buildPage(character: LegacyCharacter) {
    val section = el("relationship-network-section")
    section.append {
        div {
            id = "character-nav"
            button {
                +"Back"
                onClickFunction = {
                    characterDetail(character)
                }
            }
        }
        div { id = "relationship-network-canvas" }
    }
}

private fun buildNetwork(character: LegacyCharacter) {
    val container = el("relationship-network-canvas") as HTMLElement
    val nodes = character.friendships.mapNotNull { getCharacter(it.relativeId) }.mapIndexed { i, it -> Node(i, it.snapshots.last().name) }.toTypedArray()

//    val nodes = (1..5).map { Node(it, "Node $it") }.toTypedArray()
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