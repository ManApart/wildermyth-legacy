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
class Edge(val from: Int, val to: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Edge &&
                ((from == other.from && to == other.to) ||
                        (from == other.to && to == other.from))
    }

    override fun hashCode(): Int {
        return from + to
    }
}

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
        div { id = "relationship-network-canvas" }
        div {
            id = "network-nav"
            button {
                +"Back"
                onClickFunction = {
                    characterDetail(character)
                }
            }
        }
    }
}

private fun buildNetwork(character: LegacyCharacter) {
    val container = el("relationship-network-canvas") as HTMLElement

    val friends = findAllFriends(character)
    val nodes = buildNodes(friends)
    val edges = buildEdges(friends)

    buildNetwork(container, nodes, edges)
}


private fun findAllFriends(character: LegacyCharacter): Set<LegacyCharacter> {
    val checked = mutableSetOf<LegacyCharacter>()
    val newOptions = ArrayDeque<LegacyCharacter>()
    newOptions.add(character)
    while (newOptions.size > 0) {
        val option = newOptions.removeLast()
        checked.add(option)
        val friends = option.friendships.mapNotNull { getCharacter(it.relativeId) }
        newOptions.addAll(friends.filterNot { checked.contains(it) })
    }

    return checked
}

private fun buildNodes(friends: Set<LegacyCharacter>): Array<Node> {
    return friends.mapIndexed { i, it -> Node(i, it.snapshots.last().name) }.toTypedArray()
}

private fun buildEdges(friends: Set<LegacyCharacter>): Array<Edge> {
    val lookup = friends.mapIndexed { i, character -> character.uuid to i }.toMap()
    return friends.mapIndexed { i, character ->
        character.friendships.mapNotNull { friendship ->
            lookup[friendship.relativeId]?.let { Edge(i, it) }
        }
    }.flatten().toSet().toTypedArray()
}

private fun buildNetwork(container: HTMLElement, nodes: Array<Node>, edges: Array<Edge>) {
    val visData = VisData
    val visNet = Vis
    val data = Data(js("new visData.DataSet(nodes)"), js("new visData.DataSet(edges)"))
    val options = Options()
    js("new visNet.Network(container, data, options);")
}