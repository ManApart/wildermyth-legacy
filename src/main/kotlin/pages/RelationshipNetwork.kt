package pages

import LegacyCharacter
import Vis
import VisData
import clearSections
import el
import getCroppedHeadWithId
import getDepth
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.div
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.get
import saveDepth
import kotlin.js.Promise

class Node(
    @JsName("id") val id: Int,
    @JsName("label") val label: String,
    @JsName("image") var image: String,
    @JsName("shape") val shape: String = "circularImage"
)

data class Edge(
    @JsName("from") val from: Int,
    @JsName("to") val to: Int,
    @JsName("dashes") val dashes: Boolean = false,
    @JsName("arrows") val arrows: String? = undefined
) {
    override fun equals(other: Any?): Boolean {
        return other is Edge &&
                ((from == other.from && to == other.to) ||
                        (from == other.to && to == other.from))
    }

    override fun hashCode(): Int {
        return from + to
    }
}

fun buildRelationshipNetwork(character: LegacyCharacter, familyOnly: Boolean = false, depth: Int = getDepth()) {
    val snapshot = character.snapshots.last()
    clearSections()
    document.title = snapshot.name
    document.documentElement?.scrollTop = 0.0
    if (familyOnly) {
        window.history.pushState(null, "null", "#family/" + character.uuid)
    } else {
        window.history.pushState(null, "null", "#network/" + character.uuid)
    }
    setFavicon(character)
    buildPage(character, familyOnly)
    buildNetwork(character, familyOnly, depth)
}

private fun buildPage(character: LegacyCharacter, familyOnly: Boolean) {
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
            if (!familyOnly) {
                span {
                    id = "snapshot-span"
                    label { +"Depth:" }
                    select {
                        id = "depth-select"
                        (1..5).forEach {
                            option {
                                +"$it"
                                selected = it == getDepth()
                            }
                        }
                        option {
                            +"50"
                            selected = 50 == getDepth()
                        }

                        onChangeFunction = {
                            val input = el<HTMLSelectElement>(id)
                            val i = input.selectedIndex
                            saveDepth(input.options[i]?.textContent?.toIntOrNull() ?: 2)
                            buildRelationshipNetwork(character, familyOnly, i)
                        }
                    }
                }
            }
        }
    }
}

private fun buildNetwork(character: LegacyCharacter, familyOnly: Boolean, depth: Int) {
    val container = el("relationship-network-canvas")

    val friends = if (familyOnly) character.findAllRelatives(depth) else character.findAllFriends(depth)
    Promise.all(friends.map { getCroppedHeadWithId(it, 35.0, 45.0, 120.0, 135.0) }.toTypedArray()).then { heads ->
        val (nodeLookup, nodes) = buildNodes(friends, heads.toMap())
        val edges = if (familyOnly) buildFamilyEdges(friends) else buildFriendEdges(friends)

        buildNetwork(container, nodeLookup, nodes, edges)
    }
}

private fun buildNodes(friends: Set<LegacyCharacter>, headLookup: Map<String, String?>): Pair<Map<Int, LegacyCharacter>, Array<Node>> {
    val lookup = mutableMapOf<Int, LegacyCharacter>()
    val nodes = friends.mapIndexed { i, it ->
        val pic = headLookup[it.uuid] ?: ""
        lookup[i] = it
        Node(i, it.snapshots.last().name, pic)
    }.toTypedArray()
    return Pair(lookup, nodes)
}

private fun buildFriendEdges(friends: Set<LegacyCharacter>): Array<Edge> {
    val lookup = friends.mapIndexed { i, character -> character.uuid to i }.toMap()
    val found = mutableSetOf<String>()
    return friends.mapIndexed { i, character ->
        character.friendships.mapNotNull { friendship ->
            if (!found.contains(friendship.relativeId)) {
                found.add(friendship.relativeId)
                lookup[friendship.relativeId]?.let { Edge(i, it) }
            } else null
        }
    }.flatten().toSet().toTypedArray()
}

private fun buildFamilyEdges(relatives: Set<LegacyCharacter>): Array<Edge> {
    val lookup = relatives.mapIndexed { i, character -> character.uuid to i }.toMap()
    val found = mutableSetOf<String>()
    return relatives.mapIndexed { i, character ->
        val family = character.snapshots.last().family
        val edges = family.children.mapNotNull { child -> lookup[child]?.let { Edge(i, it, false, "to") } }.toMutableList()
        if (family.soulMate != null && !found.contains(family.soulMate)) {
            found.add(family.soulMate)
            lookup[family.soulMate]?.let { edges.add(Edge(i, it, true)) }
        }
        edges
    }.flatten().toSet().toTypedArray()
}

private fun buildNetwork(container: HTMLElement, lookup: Map<Int, LegacyCharacter>, nodes: Array<Node>, edges: Array<Edge>) {
    val visData = VisData
    val visNet = Vis
    val options = getOptions()
    val data = js("{nodes: new visData.DataSet(nodes), edges: new visData.DataSet(edges)}")
    val network = js("new visNet.Network(container, data, options)") as Vis.Network
    network.on("selectNode") { event ->
        val selectedNode = (event["nodes"] as Array<Number>).first()
        lookup[selectedNode]?.let {
            characterDetail(it)
        }
    }
}

private fun getOptions(): dynamic {
    return js(
        """{
        nodes: {
      borderWidth: 4,
      size: 30,
      color: {
        border: "#222222",
        background: "#666666",
      },
      font: { color: "#222222" },
    },
    edges: {
      color: "#222222",
    }
    }"""
    )
}