package pages

import GraphDataEntry
import Hook
import LegacyCharacter
import Profile
import Stat
import el
import format
import getCharacters
import getCompanies
import getPicture
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.div
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import searchOptions


private val mysticColor = "#38C9D5"
private val hunterColor = "#25744D"
private val warriorColor = "#D8660F"

fun TagConsumer<HTMLElement>.buildCharts(profile: Profile) {
    div(classes = "profile-section") {
        id = "profile-aggregates"
        h2 {
            +"Character Stats"
        }
        div("profile-charts") {
            id = "profile-charts"
            val legacyTier = getCharacters().groupBy { it.legacyTierLevel }.entries.sortedBy { it.key.ordinal }.map { (level, list) -> GraphDataEntry(level.format(), list.size) }
            chartTable("legacy-tier-chart", legacyTier, listOf("Level", "Count"), "Characters by Legacy Tier", "large-label") {
                searchOptions.searchText = legacyTier[it].rowName
                window.location.hash = "#"
            }

            val gender = getCharacters().groupBy { it.snapshots.last().sex }.entries.map { (level, list) -> GraphDataEntry(level.format(), list.size) }
            chartTable("gender-chart", gender, listOf("Sex", "Count"), "Characters by Sex", "small-label") {
                searchOptions.searchText = gender[it].rowName
                window.location.hash = "#"
            }

            val byClass = getCharacters().groupBy { it.snapshots.last().characterClass }.entries.map { (level, list) -> GraphDataEntry(level.format(),list.size) }
            chartTable("character-class-chart", byClass, listOf("Class", "Count"), "Characters by Class", "small-label") {
                searchOptions.searchText = byClass[it].rowName
                window.location.hash = "#"
            }

            val byPersonality = getCharacters().groupBy { it.snapshots.last().personalityFirst }.entries.sortedBy { it.key.name }.map { (level, list) -> GraphDataEntry(level.format(), list.size) }
            chartTable("personality-chart", byPersonality, listOf("Personality", "Count"), "Characters by Top Personality", "small-label") {
                searchOptions.searchText = byPersonality[it].rowName
                window.location.hash = "#"
            }

            val byPersonalitySecond = getCharacters().groupBy { it.snapshots.last().personalitySecond }.entries.sortedBy { it.key.name }.map { (level, list) -> GraphDataEntry(level.format(), list.size) }
            chartTable("personality-second-chart", byPersonalitySecond, listOf("Personality", "Count"), "Characters by Second Personality", "small-label") {
                searchOptions.searchText = byPersonalitySecond[it].rowName
                window.location.hash = "#"
            }

            val byTheme = mutableMapOf<String, Pair<String, Int>>()
            getCharacters().forEach { char ->
                char.snapshots.last().themes.forEach { theme ->
                    byTheme[theme.id] = Pair(theme.name, (byTheme[theme.id]?.second ?: 0) + 1)
                }
            }
            val byThemeSorted = byTheme.entries.sortedBy { it.value.first }.map { (key, value) -> GraphDataEntry(value.first, value.second, rowSearch = key) }
            chartTable("theme-chart", byThemeSorted, listOf("Theme", "Count"), "Characters by Theme", "large-label") {
                searchOptions.searchText = byThemeSorted[it].rowSearch ?: ""
                window.location.hash = "#"
            }

            val popularity = getCharacters().map { it to it.friendships.size }.filter { it.second > 1 }.sortedByDescending { it.second }.take(20).toMap()
            chartTable("popularity-chart", popularity, listOf("Character", "Friend Count"), "Popularity (Relationship Count)", "large-label")

            val campaigns = getCharacters().map { it to it.companyIds.size }.filter { it.second > 1 }.sortedByDescending { it.second }.take(20).toMap()
            chartTable("campaigns-chart", campaigns, listOf("Character", "Campaign Count"), "Campaigns Participated In", "large-label")

            val kills = getCharacters().map { it to it.killCount }.filter { it.second > 10 }.sortedByDescending { it.second }.take(20).toMap()
            chartTable("kills-chart", kills, listOf("Character", "Kills"), "Confirmed Kills", "large-label")

            val enemyTypes = getCompanies().groupBy {
                it.mainThreat
            }.map { (group, campaigns) -> GraphDataEntry(group.capitalize(), campaigns.size, "images/foe/$group.png") }.sortedBy { it.rowName }
            chartTableWithPic("enemy-campaign-count", enemyTypes, listOf("Group", "Runs"), "Campaigns Against Enemy", "small-label")

            val enemyKills = listOfNotNull(
                profile.unlocks.firstOrNull { it.id == "achievementProgress_cultistKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_drauvenKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_gorgonKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_morthagiKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_thrixlKills" },
            ).map { it.name.replace("Kills", "").trim() to it.progress }
                .sortedBy { it.first }
                .map { (key, value) -> GraphDataEntry(key, value,"images/foe/${key.lowercase()}.png") }
            chartTableWithPic("enemy-kill-count", enemyKills, listOf("Group", "Kills"), "Enemies Killed", "small-label")

        }

        div("profile-charts") {
            div("profile-chart-wrapper") {
                id = "skill-table-section"
            }
            div("profile-chart-wrapper") {
                id = "hooks-table-section"
            }
        }

    }
}

fun buildSmartCharts() {
    skillTable(el("skill-table-section"), Stat.ARMOR)
    hooksTable(el("hooks-table-section"), HookType.RESOLVED)
}

private fun skillTable(parent: HTMLElement, stat: Stat) {
    parent.innerHTML = ""
    parent.append {
        div("profile-charts") {
            div("smart-chart-wrapper") {
                div {
                    id = "start-skill-select-span"
                    label { +"Skill:" }
                    select {
                        id = "starting-skill-select"
                        Stat.values().forEach {
                            option {
                                +it.format()
                                selected = stat == it
                            }
                        }
                        onChangeFunction = {
                            val optionI = (document.getElementById(id) as HTMLSelectElement).selectedIndex
                            skillTable(parent, Stat.values()[optionI])
                        }
                    }
                }

                val bestStat = getCharacters().map { it to (it.snapshots.last().primaryStats[stat] ?: 0f) }.sortedByDescending { it.second }.take(10).toMap()
                chartTable("best-stat-chart", bestStat, listOf("Character", stat.format()), "Highest Starting ${stat.format()}", "large-label")
            }
        }
    }
}

enum class HookType { UNRESOLVED, RESOLVED, ALL }

private fun hooksTable(parent: HTMLElement, hookType: HookType) {
    parent.innerHTML = ""
    parent.append {
        div("profile-charts") {
            div("smart-chart-wrapper") {
                div {
                    id = "hooks-select-span"
                    label { +"Type:" }
                    select {
                        id = "starting-hook-select"
                        HookType.values().forEach {
                            option {
                                +it.format()
                                selected = hookType == it
                            }
                        }
                        onChangeFunction = {
                            val optionI = (document.getElementById(id) as HTMLSelectElement).selectedIndex
                            hooksTable(parent, HookType.values()[optionI])
                        }
                    }
                }

                val hookSort: (Hook) -> Boolean = when (hookType) {
                    HookType.UNRESOLVED -> { hook -> !hook.resolved }
                    HookType.RESOLVED -> { hook -> hook.resolved }
                    HookType.ALL -> { _ -> true }
                }

                val byHook =
                    getCharacters().flatMap { char -> char.hooks.filter { hookSort(it) } }.groupBy { it.id }.entries.sortedBy { it.key }.map { (hook, list) -> GraphDataEntry(hook.format(), list.size) }
                chartTable("hook-resolved-chart", byHook, listOf("Hook", "Count"), "Hooks", "small-label") {
                    searchOptions.searchText = byHook[it].rowName
                    window.location.hash = "#"
                }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.chartTable(docId: String, data: Map<LegacyCharacter, Number>, headers: List<String>, caption: String, labelClass: String) {
    val namedData = data.entries.map { (character, amount) ->
        val name = character.snapshots.last().name
        val picUrl = getPicture("${character.uuid}/head")
        val color = when(character.snapshots.last().characterClass){
            CharacterClass.MYSTIC -> mysticColor
            CharacterClass.WARRIOR -> warriorColor
            CharacterClass.HUNTER -> hunterColor
        }
        GraphDataEntry(name, amount.toFloat(), picUrl, color)
    }
    chartTableWithPic(docId, namedData, headers, caption, labelClass) { characterDetail(data.keys.toList()[it]) }
}

private fun TagConsumer<HTMLElement>.chartTable(docId: String, data: List<GraphDataEntry>, headers: List<String>, caption: String, labelClass: String, onClick: (Int) -> Unit = {}) {
    chartTableWithPic(docId, data, headers, caption, labelClass, onClick)
}

private fun TagConsumer<HTMLElement>.chartTableWithPic(
    docId: String,
    data: List<GraphDataEntry>,
    headers: List<String>,
    caption: String,
    labelClass: String,
    onClick: (Int) -> Unit = {}
) {
    val max = data.map { it.amount }.maxOfOrNull { it }
    if (max != null && max != 0f) {
        div("profile-chart-wrapper") {
            table("charts-css bar show-heading show-labels labels-align-end data-spacing-4 profile-chart") {
                id = docId
                caption { +caption }
                thead {
                    tr {
                        headers.forEach {
                            th(ThScope.col) { +it }
                        }
                    }
                }
                tbody {
                    data.forEachIndexed { i, data ->
                        tr(classes = labelClass) {
                            th(ThScope.row, classes = "profile-header $labelClass") {
                                if (data.picUrl != null) {
                                    img(classes = "label-image") {
                                        src = data.picUrl
                                    }
                                }
                                +data.rowName
                            }
                            td {
                                val colorString = if(data.color != null) " --color: ${data.color};" else ""
                                style = "--size: calc( ${data.amount} / $max );$colorString"
                                +"${data.amount}"
                            }
                            onClickFunction = { onClick(i) }
                        }
                    }
                }
            }
        }
    }
}