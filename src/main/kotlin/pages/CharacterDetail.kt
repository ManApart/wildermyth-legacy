package pages

import AdditionalInfo
import Aspect
import Character
import Gear
import HistoryEntry
import LegacyCharacter
import clearSections
import favicon
import getAdditionalInfo
import getCharacter
import getCharacters
import getCompany
import getPicture
import jsonMapper
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.*
import kotlinx.html.js.div
import org.w3c.dom.*
import saveAdditionalInfo
import kotlin.js.Date
import kotlinx.serialization.encodeToString
import org.w3c.dom.events.KeyboardEvent

private lateinit var currentCharacter: LegacyCharacter
fun characterDetail(character: LegacyCharacter, snapshot: Character = character.snapshots.last(), showAggregates: Boolean = true) {
    currentCharacter = character
    val additionalInfo = getAdditionalInfo(character.uuid)
    val section = document.getElementById("character-detail-section")!!
    clearSections()
    document.title = snapshot.name
    document.documentElement?.scrollTop = 0.0
    window.history.pushState(null, "null", "#detail/" + character.uuid)
    setFavicon(character)

    section.append {
        buildNav(character, showAggregates, snapshot)
        div {
            id = "character-details"
            characterCard(character, snapshot, false)
            div("details-subsection") {
                statsSection(character, snapshot)
                companiesSection(character)
            }
            familySection(snapshot)
            if (showAggregates) {
                friendshipSection(character)
            } else {
                friendshipSection(character, snapshot)
            }
            gearSection(snapshot)
            customHistorySection(additionalInfo)
            gameHistorySection(snapshot)
            aspectsSection(snapshot)
            fullHistorySection(snapshot)
        }
    }
}

private fun TagConsumer<HTMLElement>.buildNav(character: LegacyCharacter, showAggregates: Boolean, snapshot: Character) {
    div {
        id = "character-nav"
        button {
            +"Back"
            onClickFunction = {
                characterDetail(previousCharacter(character))
            }
        }
        button {
            +"List"
            onClickFunction = {
                window.location.hash = "#${character.uuid}"
            }
        }
        button {
            +"Next"
            onClickFunction = {
                characterDetail(nextCharacter(character))
            }
        }
        span {
            id = "snapshot-span"
            label { +"Snapshot:" }
            select {
                id = "snapshot-select"
                character.snapshots.forEach {
                    option {
                        +"${it.name}: ${it.age}yrs"
                        selected = showAggregates == false && snapshot == it
                    }
                }
                option {
                    +"All"
                    selected = showAggregates
                }

                onChangeFunction = {
                    val snapshotI = (document.getElementById(id) as HTMLSelectElement).selectedIndex
                    changeSnapshot(snapshotI, character)
                }
            }
        }
        button {
            id = "log-button"
            +"Log Detail"
            onClickFunction = {
                println(jsonMapper.encodeToString(character))
            }
        }
    }
}

fun onKeyDown(key: KeyboardEvent) {
    if (window.location.hash.contains("detail")) {
        key.preventDefault()
    }
}

fun onKeyUp(key: KeyboardEvent) {
    if (window.location.hash.contains("detail")) {
        when (key.key) {
            "ArrowLeft" -> characterDetail(previousCharacter(currentCharacter))
            "ArrowRight" -> characterDetail(nextCharacter(currentCharacter))
            "ArrowUp" -> previousSnapshot(currentCharacter)
            "ArrowDown" -> nextSnapshot(currentCharacter)
        }
    }
}

fun setFavicon(character: LegacyCharacter) {
    getPicture("${character.uuid}/head")?.let { picture ->
        val width = 100.0
        val height = 115.0

        val image = Image().apply { src = picture }

        val canvas = (document.createElement("canvas") as HTMLCanvasElement)
        canvas.width = width.toInt()
        canvas.height = height.toInt()
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.drawImage(image, 45.0, 55.0, width, height, 0.0, 0.0, width, height)
        val cropped = canvas.toDataURL("image/png", 0.9)

        favicon.setAttribute("href", cropped)
    }
}

private fun nextCharacter(character: LegacyCharacter): LegacyCharacter {
    val characters = getCharacters().sorted()
    val next = characters.indexOf(character) + 1
    val i = if (next >= characters.size) 0 else next
    return characters[i]
}

private fun previousCharacter(character: LegacyCharacter): LegacyCharacter {
    val characters = getCharacters().sorted()
    val previous = characters.indexOf(character) - 1
    val i = if (previous < 0) characters.size - 1 else previous
    return characters[i]
}

private fun previousSnapshot(character: LegacyCharacter) {
    val snapshotI = (document.getElementById("snapshot-select") as HTMLSelectElement).selectedIndex - 1
    val i = if (snapshotI < 0) character.snapshots.size else snapshotI
    changeSnapshot(i, character)
}

private fun nextSnapshot(character: LegacyCharacter) {
    val snapshotI = (document.getElementById("snapshot-select") as HTMLSelectElement).selectedIndex + 1
    val i = if (snapshotI > character.snapshots.size) 0 else snapshotI
    changeSnapshot(i, character)
}

private fun changeSnapshot(snapshotI: Int, character: LegacyCharacter) {
    val nowShowAggregates = snapshotI >= character.snapshots.size
    if (nowShowAggregates) {
        characterDetail(character, character.snapshots.last(), true)
    } else {
        characterDetail(character, character.snapshots[snapshotI], false)
    }
}

fun TagConsumer<HTMLElement>.customHistorySection(additionalInfo: AdditionalInfo) {
    div("character-section") {
        id = "custom-history-entries"
        h2 { +"Journal" }
        additionalInfo.history.forEachIndexed { i, entry ->
            div("custom-history-entry") {

                textArea {
                    id = "custom-history-$i"
                    +" ${entry.textOverride}"
                    onChangeFunction = {
                        val area = document.getElementById(id) as HTMLTextAreaElement
                        entry.textOverride = area.value
                        saveAdditionalInfo(additionalInfo)
                    }
                }
            }
        }
        button {
            +"Add"
            onClickFunction = {
                val entry = HistoryEntry("" + additionalInfo.history.size, Date().getTime().toLong(), "")
                additionalInfo.history.add(entry)
                val node = document.getElementById("custom-history-entries")!!
                node.innerHTML = ""
                node.append { customHistorySection(additionalInfo) }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.gameHistorySection(character: Character) {
    div("character-section") {
        id = "game-history-entries"
        h2 { +"Game History" }
        div {
            id = "game-history-wrapper"
            div {
                id = "game-history-inner"
                character.history.filter { it.showInSummary }.forEachIndexed { i, entry ->
                    div("game-history-entry") {
                        div("game-history-entry-inner") {
                            p {
                                id = "game-history-$i"
                                +" ${entry.getText(character)}"
                            }
                        }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.fullHistorySection(character: Character) {
    val firstHalf = character.history.subList(0, character.history.size / 2)
    val secondHalf = character.history.subList(character.history.size / 2, character.history.size)
    div("character-section") {
        id = "full-history-entries"
        h2 { +"Full History" }
        div {
            id = "full-history-columns"
            buildHistorySection(character, firstHalf, "left")
            buildHistorySection(character, secondHalf, "right")
        }
    }
}

private fun DIV.buildHistorySection(character: Character, history: List<HistoryEntry>, side: String) {
    div("full-history-column") {
        id = "full-history-column-$side"
        history.forEachIndexed { i, entry ->
            div("full-history-entry") {
                div("full-history-entry-inner") {
                    p {
                        id = "full-history-$i"
                        +" ${entry.getText(character)}"
                    }
                    if (entry.associatedAspects.isNotEmpty()) {
                        p {
                            +"Aspects: ${entry.associatedAspects.joinToString { it.name }}"
                        }
                    }
                    if (entry.forbiddenAspects.isNotEmpty()) {
                        p {
                            +"Forbids: ${entry.forbiddenAspects.joinToString { it.name }}"
                        }
                    }
                    val relates = entry.relationships.filter { it.name != null }
                    if (relates.isNotEmpty()) {
                        p {
                            +"Relates: ${relates.joinToString { it.name ?: "" }}"
                        }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.statsSection(legacyCharacter: LegacyCharacter, snapshot: Character) {
    val rawStats = snapshot.aspects.filter { it.name == "historyStat2" }
    div("character-section") {
        id = "stats-section"
        div {
            id = "personality-section"
            h2 { +"Personality" }
            table {
                tbody {
                    snapshot.personality.entries.sortedByDescending { it.value }.forEach { (type, amount) ->
                        tr {
                            td { +type.format() }
                            td { +"$amount" }
                        }
                    }
                }
            }
        }
        div {
            id = "stat-bonuses"
            h2 { +"Stat Bonuses" }
            table {
                tbody {
                    rawStats.sortedBy { it.name }.forEach { stat ->
                        val statName = stat.values.first().format()
                        val statVal = stat.values[1]
                        tr {
                            td { +statName }
                            td { +statVal }
                        }
                    }
                }
            }
        }
        div {
            id = "misc-stats"
            h2 { +"Misc" }
            table {
                tbody {
                    tr {
                        td { +"Hometown" }
                        td { +snapshot.hometown }
                    }
                    tr {
                        td { +"Kills" }
                        td { +"${legacyCharacter.killCount}" }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.familySection(snapshot: Character) {
    with(snapshot.family) {
        if (soulMate != null || parents.isNotEmpty() || children.isNotEmpty()) {
            div("character-section") {
                id = "family-section"
                div {
                    h2 { +"Family" }
                    parents.forEach { relativeCard(it, "Parent") }
                    soulMate?.let { relativeCard(it, "Soulmate") }
                    children.forEach { relativeCard(it, "Child") }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.friendshipSection(character: LegacyCharacter) {
    if (character.friendships.isNotEmpty()) {
        div("character-section") {
            id = "friendships-section"
            div {
                relationshipHeader(character)
                character.friendships.forEach { friendShip ->
                    relativeCard(friendShip.relativeId, friendShip.kind.getTitle(friendShip.level), friendShip.level)
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.friendshipSection(character: LegacyCharacter, snapshot: Character) {
    if (snapshot.friendships.isNotEmpty()) {
        div("character-section") {
            id = "friendships-section"
            div {
                relationshipHeader(character)
                snapshot.friendships.forEach { friendShip ->
                    relativeCard(friendShip.relativeId, friendShip.kind.getTitle(friendShip.level), friendShip.level)
                }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.relationshipHeader(character: LegacyCharacter) {
    h2("relationship-header") {
        +"Relationships"
        onClickFunction = { buildRelationshipNetwork(character) }
        img {
            classes = setOf("network-image")
            src = "./network.png"
        }
    }
}

fun TagConsumer<HTMLElement>.companiesSection(character: LegacyCharacter) {
    div("character-section") {
        id = "companies-section"
        h2 { +"Companies" }
        character.companyIds.forEach { companyCard(it) }
    }
}

private fun TagConsumer<HTMLElement>.relativeCard(relativeUuid: String, relationship: String, rank: Int? = null) {
    val relative = getCharacter(relativeUuid)
    val snapshot = relative?.snapshots?.last()

    if (relative != null && snapshot != null) {
        div("relationship") {
            div("relationship-inner") {
                onClickFunction = { characterDetail(relative) }
                getPicture("$relativeUuid/head")?.let { picture ->
                    img(classes = "relationship-pic") {
                        src = picture
                    }
                }
                div("relationship-text") {
                    div {
                        h4 { +relationship }
                        if (rank != null) p("relationship-rank") { +"($rank)" }
                    }
                    p { +snapshot.name }
                }
            }
        }
    } else {
        div {
            h4 { +relationship }
            p { +relativeUuid }
        }
    }
}

private fun TagConsumer<HTMLElement>.companyCard(companyId: String) {
    val company = getCompany(companyId)

    div("company") {
        div {
            h4 { +company.name }
            p("company-foe") { +"Foe: ${company.mainThreat.capitalize()}" }
        }
        company.characters.mapNotNull { getCharacter(it) }.forEach { relative ->
            getPicture("${relative.uuid}/head")?.let { picture ->
                div("company-friend-pic-wrapper") {
                    img(classes = "company-friend-pic") {
                        src = picture
                        alt = relative.snapshots.last().name
                        onClickFunction = { characterDetail(relative) }
                    }
                }
            }
        }

    }
}

fun TagConsumer<HTMLElement>.aspectsSection(character: Character) {
    val allAspects = character.aspects.filterNot { it.hiddenAspect() }.sortedBy { it.name }
    val firstHalf = allAspects.subList(0, allAspects.size / 2)
    val secondHalf = allAspects.subList(allAspects.size / 2, allAspects.size)
    div("character-section") {
        id = "aspects-section"
        h2 { +"Aspects" }
        div {
            id = "aspect-tables"
            buildAspectTable(firstHalf)
            buildAspectTable(secondHalf)
        }
    }
}

private fun DIV.buildAspectTable(firstHalf: List<Aspect>) {
    div("aspects-table") {
        table {
            tbody {
                firstHalf.forEach { aspect ->
                    tr {
                        td { +aspect.name }
                        td { +aspect.values.joinToString(", ") }
                    }
                }
            }
        }
    }
}

private fun Aspect.hiddenAspect(): Boolean {
    return name in listOf("familyWith", "childOf", "parentOf", "historyStat2", "roleStats")
            || name.startsWith("human")
            || name.startsWith("relationship")
}

private fun TagConsumer<HTMLElement>.gearSection(snapshot: Character) {
    div("character-section") {
        id = "gear-section"
        h2 { +"Gear" }
        snapshot.gear.forEach { gearCard(it) }
    }
}

private fun TagConsumer<HTMLElement>.gearCard(gear: Gear) {
    with(gear) {
        div("gear") {
            h4 { +name }
            p("item-id") { +"($itemId)" }
            div("gear-details") {
                val artifactText = if (artifact) " Artifact" else ""
                val subCatText = if (subCategory != null) " ($subCategory)" else ""
                val equipVerb = if (isEquipped) "Equipped" else "Equips"
                val aspectText = ownerAspects
                    .filter { !it.name.startsWith("slotFilled") }
                    .joinToString(", ") {
                        val values = if (it.values.isEmpty()) "" else "(${it.values.joinToString(",")})"
                        it.name + values
                    }

                p { +"Level $tier$artifactText ${category.capitalize()} $subCatText" }
                p { +"$equipVerb to ${slots.joinToString(", ") { it.lowercase().replace("augment_", "") }.capitalize()}" }
                p { +"Grants $aspectText" }
            }
        }
    }
}