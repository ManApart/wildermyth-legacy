package pages

import Ability
import AdditionalInfo
import Aspect
import Character
import Company
import Gear
import HistoryEntry
import LegacyCharacter
import clearSections
import el
import favicon
import getAdditionalInfo
import getCharacter
import getCharacters
import getCompany
import getCompanyForGameId
import getCroppedHead
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
import kotlin.math.abs

private lateinit var currentCharacter: LegacyCharacter
fun characterDetail(character: LegacyCharacter, snapshot: Character = character.snapshots.last(), showAggregates: Boolean = true, startY: Double = 0.0) {
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
                statsSection(character, snapshot, showAggregates)
                tagsSection(additionalInfo)
                companiesSection(character)
            }
            familySection(character, snapshot)
            if (showAggregates) {
                friendshipSection(character)
            } else {
                friendshipSection(character, snapshot)
            }
            compatibilitySection(character)
            abilitiesSection(snapshot)
            gearSection(snapshot)
            customHistorySection(additionalInfo)
            gameHistorySection(snapshot)
            aspectsSection(snapshot)
            fullHistorySection(snapshot)
        }
    }
    window.scrollTo(0.0, startY)
}

private fun TagConsumer<HTMLElement>.buildNav(character: LegacyCharacter, showAggregates: Boolean, snapshot: Character) {
    div {
        id = "character-nav"
        button {
            +"Back"
            title = "You can also use left/right arrow keys."
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
            title = "You can also use left/right arrow keys."
            onClickFunction = {
                characterDetail(nextCharacter(character))
            }
        }
        span {
            id = "snapshot-span"
            title = "You can also use up/down arrow keys."
            label { +"Snapshot:" }
            select {
                id = "snapshot-select"
                character.snapshots.forEach {
                    option {
                        val company = getCompanyForGameId(it.gameId)
                        +company.name
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
                println("Raw Json (not saved; only exists after import):")
                println(character.rawJson)
                println("Parsed Character:")
                println(jsonMapper.encodeToString(character))
            }
        }
    }
}

fun onKeyDown(key: KeyboardEvent) {
    if (window.location.hash.contains("detail")) {
        if (key.key in listOf("ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown")) {
            key.preventDefault()
        }
    }
}

fun onKeyUp(key: KeyboardEvent) {
    if (window.location.hash.contains("detail")) {
        when (key.key) {
            "ArrowLeft" -> characterDetail(previousCharacter(currentCharacter), startY = window.pageYOffset)
            "ArrowRight" -> characterDetail(nextCharacter(currentCharacter), startY = window.pageYOffset)
            "ArrowUp" -> previousSnapshot(currentCharacter)
            "ArrowDown" -> nextSnapshot(currentCharacter)
        }
    }
}

fun setFavicon(character: LegacyCharacter) {
    getCroppedHead(character).then {
        it?.let { cropped ->
            println("Favicon for ${character.snapshots.last().name}")
            favicon.setAttribute("href", cropped)
        } ?: println("Unable to find favicon!")
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
        characterDetail(character, character.snapshots.last(), true, window.pageYOffset)
    } else {
        characterDetail(character, character.snapshots[snapshotI], false, window.pageYOffset)
    }
}

fun TagConsumer<HTMLElement>.customHistorySection(additionalInfo: AdditionalInfo) {
    div("character-section") {
        id = "custom-history-entries"
        h2 {
            +"Journal"
            title = "Add your own notes here. They're saved to your browser. You can also export them from the main page to save them locally or transfer them to another browser."
        }
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

fun TagConsumer<HTMLElement>.statsSection(legacyCharacter: LegacyCharacter, snapshot: Character, showAggregates: Boolean) {
    val rawStats = snapshot.aspects.filter { it.name == "historyStat2" }
    val hooks = if (showAggregates) legacyCharacter.hooks else snapshot.hooks
    div("character-section") {
        id = "stats-section"
        div {
            id = "personality-section"
            h2 {
                +"Personality"
                onClickFunction = {
                    window.open("https://wildermyth.com/wiki/Stat#Personality_Stats", "_blank")
                }
            }
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
                    hooks.forEach { hook ->
                        tr {
                            td {
                                +"Hook: ${hook.id}"
                                onClickFunction = {
                                    window.open("https://wildermyth.com/wiki/Category:${hook.id}", "_blank")
                                }
                            }
                            td { + if (hook.resolved) "Resolved" else "" }
                        }
                    }
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.tagsSection(info: AdditionalInfo) {
    div("character-section") {
        id = "tags-section"
        h2 { +"Tags" }
        div {
            id = "tag-display"
            displayTags(info)
        }
        input {
            id = "add-tag-input"
            type = InputType.text
            placeholder = "New tag"
            onKeyUpFunction = { e ->
                val key = (e as KeyboardEvent)
                if (key.key == "Enter") {
                    saveTag(info)
                }
            }
        }
        img {
            src = "./images/plus-circle.svg"
            onClickFunction = {
                saveTag(info)
            }
        }
    }
}

private fun saveTag(info: AdditionalInfo) {
    val input = el<HTMLInputElement>("add-tag-input")
    val display = el<HTMLDivElement>("tag-display")
    val tag = input.value
    if (tag.isNotBlank()) {
        info.tags.add(tag)
        input.value = ""
        saveAdditionalInfo(info)
        refreshTagDisplay(display, info)
    }
}

private fun refreshTagDisplay(div: HTMLDivElement, info: AdditionalInfo) {
    div.innerHTML = ""
    div.append { displayTags(info) }
}

private fun TagConsumer<HTMLElement>.displayTags(info: AdditionalInfo) {
    info.tags.forEach { tag ->
        span("tag-label") {
            +tag
            img {
                src = "./images/x-circle.svg"
                onClickFunction = {
                    info.tags.remove(tag)
                    saveAdditionalInfo(info)
                    refreshTagDisplay(el<HTMLDivElement>("tag-display"), info)
                }
            }
        }
    }
}

fun TagConsumer<HTMLElement>.familySection(character: LegacyCharacter, snapshot: Character) {
    with(snapshot.family) {
        if (soulMate != null || parents.isNotEmpty() || children.isNotEmpty()) {
            div("character-section") {
                id = "family-section"
                div {
                    familyHeader(character)
                    parents.forEach { relativeCard(snapshot, it, "Parent") }
                    soulMate?.let { relativeCard(snapshot, it, "Soulmate") }
                    children.forEach { relativeCard(snapshot, it, "Child") }
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
                    relativeCard(character.snapshots.last(), friendShip.relativeId, friendShip.kind.getTitle(friendShip.level), friendShip.level)
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
                    relativeCard(snapshot, friendShip.relativeId, friendShip.kind.getTitle(friendShip.level), friendShip.level)
                }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.familyHeader(character: LegacyCharacter) {
    h2("relationship-header") {
        +"Family"
        title = "${character.snapshots.last().name}'s family. Click to see a family tree."
        onClickFunction = { buildRelationshipNetwork(character, true) }
        img {
            classes = setOf("network-image")
            src = "images/network.png"
        }
    }
}

private fun TagConsumer<HTMLElement>.relationshipHeader(character: LegacyCharacter) {
    h2("relationship-header") {
        +"Relationships"
        title = "Highest relationship levels that ${character.snapshots.last().name} has formed. Click to explore a network map."
        onClickFunction = { buildRelationshipNetwork(character) }
        img {
            classes = setOf("network-image")
            src = "images/network.png"
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

private fun TagConsumer<HTMLElement>.relativeCard(mySnapshot: Character, relativeUuid: String, relationship: String, rank: Int? = null) {
    val relative = getCharacter(relativeUuid)
    val snapshot = relative?.snapshots?.last()

    if (relative != null && snapshot != null) {
        val compatibility = mySnapshot.getCompatibility(snapshot)
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
                        val rankText = if (rank != null) "Rank $rank, " else ""
                        p("relationship-rank") { +"(${rankText}Compatibility $compatibility)" }
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

private fun TagConsumer<HTMLElement>.compatibilitySection(character: LegacyCharacter) {
    val me = character.snapshots.last()
    val levelGroups = getCharacters()
        .groupBy { me.getCompatibility(it.snapshots.last()) }
        .entries.sortedByDescending { it.key }

    div("character-section") {
        id = "compatibility-section"
        h2 {
            +"Compatibility"
            title = "How compatible ${me.name} is with other characters. More hearts means faster friendship and love. More lightning bolts means faster becoming rivals. (Click for more info)"
            onClickFunction = {
                window.open("https://wildermyth.com/wiki/Relationship", "_blank")
            }
        }
        levelGroups.forEach { (level, friends) ->
            div("compatibility-row") {
                div {
                    repeat(abs(level)) {
                        img {
                            classes = setOf("compatibility-image")
                            src = if (level > 0) "images/heart.svg" else "images/zap.svg"
                        }
                    }
                }
                friends.forEach { friend ->
                    getPicture("${friend.uuid}/head")?.let { picture ->
                        div("company-friend-pic-wrapper") {
                            img(classes = "company-friend-pic") {
                                src = picture
                                alt = friend.snapshots.last().name
                                onClickFunction = { characterDetail(friend) }
                            }
                        }
                    }
                }
            }
        }
    }

}

fun TagConsumer<HTMLElement>.companyCard(companyId: String) {
    companyCard(getCompany(companyId))
}

fun TagConsumer<HTMLElement>.companyCard(company: Company) {
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
    val allAspects = character.aspects.sortedBy { it.name }
    val firstHalf = allAspects.subList(0, allAspects.size / 2)
    val secondHalf = allAspects.subList(allAspects.size / 2, allAspects.size)
    div("character-section two-column-section") {
        id = "aspects-section"
        h2 { +"Aspects" }
        div("column-tables") {
            id = "aspect-tables"
            buildAspectTable(firstHalf)
            buildAspectTable(secondHalf)
        }
    }
}

private fun DIV.buildAspectTable(firstHalf: List<Aspect>) {
    div("column-table") {
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

private fun TagConsumer<HTMLElement>.abilitiesSection(snapshot: Character) {
    div("character-section") {
        id = "abilities-section"
        h2 { +"Abilities" }
        snapshot.abilities.forEach { abilityCard(it) }
    }
}

private fun TagConsumer<HTMLElement>.abilityCard(ability: Ability) {
    with(ability) {
        div("gear") {
            h4 {
                +name
                title = "Click to view Wiki"
                onClickFunction = {
                    val urlPart = ability.name.replace(" ", "_").replace("+", "")
                    window.open("https://wildermyth.com/wiki/$urlPart", "_blank")
                }
            }
            p("item-id") { +"(${ability.id})" }
            div("gear-details") {
                p { +description }
            }
        }
    }
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
            h4 {
                +name
                title = "Click to view Wiki"
                onClickFunction = {
                    window.open("https://wildermyth.com/wiki/Equipment", "_blank")
                }
            }
            p("item-id") { +"($itemId)" }
            div("gear-details") {
                val artifactText = if (artifact) " Artifact" else ""
                val subCatText = if (subCategory != null) " ($subCategory)" else ""
                val equipVerb = if (isEquipped) "equipped" else "equips"
                val aspectText = ownerAspects
                    .filter { !it.name.startsWith("slotFilled") }
                    .joinToString(", ") {
                        val values = if (it.values.isEmpty()) "" else "(${it.values.joinToString(",")})"
                        it.name + values
                    }

                p {
                    +"Level $tier$artifactText ${category.capitalize()} $subCatText $equipVerb to ${
                        slots.joinToString(", ") { it.lowercase().replace("augment_", "") }.capitalize()
                    } and grants $aspectText"
                }
                p { +description }
            }
        }
    }
}