package pages

import Ability
import LegacyCharacter
import Profile
import Stat
import Unlock
import clearSections
import el
import format
import getCharacters
import getCompanies
import getPicture
import getProfile
import jsonMapper
import kotlinx.serialization.encodeToString
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

fun profile() {
    val profile = getProfile()
    val section = document.getElementById("profile-section")!!
    clearSections()
    document.title = profile.name + "'s Legacy"
    document.documentElement?.scrollTop = 0.0
    window.history.pushState(null, "null", "#profile")
    setFavicon(getCharacters().random())

    section.append {
        buildProfileNav()
        buildLinks(profile)
        buildCharts(profile)
        buildCompanies()
        buildUnlocks(profile)
    }
    skillTable(el("profile-charts"), Stat.ARMOR)
}

private fun TagConsumer<HTMLElement>.buildProfileNav() {
    div {
        id = "profile-nav"
        button {
            +"List"
            onClickFunction = {
                window.location.hash = "#"
            }
        }
        button {
            id = "upload-button"
            +"Upload"
            onClickFunction = { importMenu(false) }
        }
    }
}

private fun TagConsumer<HTMLElement>.buildLinks(profile: Profile) {
    div(classes = "profile-section") {
        id = "profile-links"
        span("profile-link") {
            a("https://wildermyth.com/wiki/Relationship") {
                +"Wiki"
                target = "_blank"
            }
        }
        span("profile-link") {
            a("https://steamcommunity.com/stats/763890/achievements/") {
                +"Achievements"
                target = "_blank"
            }
        }
        span("profile-link") {
            a("https://steamcommunity.com/id/${profile.name}/stats/763890/achievements/") {
                +"Personal Achievements"
                target = "_blank"
            }
        }
        span("profile-link") {
            a("https://steamcommunity.com/app/763890/workshop/") {
                +"Mods"
                target = "_blank"
            }
        }
        span("profile-link") {
            a("https://github.com/ManApart/wildermyth-legacy") {
                +"Site Source"
                target = "_blank"
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.buildUnlocks(profile: Profile) {
    val allUnlocks = profile.unlocks.sortedBy { it.id }
    val firstHalf = allUnlocks.subList(0, allUnlocks.size / 2)
    val secondHalf = allUnlocks.subList(allUnlocks.size / 2, allUnlocks.size)
    div("profile-section two-column-section") {
        id = "profile-unlocks"
        h2 {
            +"Achievements"
            title = "Every achievement you've unlocked or are tracking."
        }
        div("column-tables") {
            id = "unlock-tables"

            buildUnlockTable(firstHalf)
            buildUnlockTable(secondHalf)
        }
    }
}

private fun TagConsumer<HTMLElement>.buildUnlockTable(unlocks: List<Unlock>) {
    div("column-table") {
        table {
            tr {
                th(classes = "count-column") { +"Count" }
                th { +"Name" }
            }
            tbody {
                unlocks.forEach { unlock ->
                    val progress = if (unlock.progress == 0) "" else unlock.progress.toString()
                    tr {
                        td(classes = "count-column") { +progress }
                        td {
                            +unlock.name
                            title = unlock.id
                        }
                    }
                }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.buildCompanies() {
    val allCompanies = getCompanies()
    val firstHalf = allCompanies.subList(0, allCompanies.size / 2)
    val secondHalf = allCompanies.subList(allCompanies.size / 2, allCompanies.size)
    div(classes = "profile-section") {
        id = "profile-companies"
        h2 {
            +"Companies"
            title = "Every company you've completed a campaign with."
        }
        div("column-tables") {
            id = "company-lists"
            div("column-table") {
                firstHalf.forEach { company ->
                    companyCard(company)
                }
            }
            div("column-table") {
                secondHalf.forEach { company ->
                    companyCard(company)
                }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.buildCharts(profile: Profile) {
    div(classes = "profile-section") {
        id = "profile-aggregates"
        h2 {
            +"Character Stats"
        }
        div("profile-charts") {
            id = "profile-charts"
            val legacyTier = getCharacters().groupBy { it.legacyTierLevel }.entries.sortedBy { it.key.ordinal }.associate { (level, list) -> level.format() to list.size }
            chartTable("legacy-tier-chart", legacyTier, listOf("Level", "Count"), "Characters by Legacy Tier") {
                searchOptions.searchText = legacyTier.keys.toList()[it]
                window.location.hash = "#"
            }

            val gender = getCharacters().groupBy { it.snapshots.last().sex }.entries.associate { (level, list) -> level.format() to list.size }
            chartTable("gender-chart", gender, listOf("Sex", "Count"), "Characters by Sex") {
                searchOptions.searchText = gender.keys.toList()[it]
                window.location.hash = "#"
            }

            val byClass = getCharacters().groupBy { it.snapshots.last().characterClass }.entries.associate { (level, list) -> level.format() to list.size }
            chartTable("character-class-chart", byClass, listOf("Class", "Count"), "Characters by Class") {
                searchOptions.searchText = byClass.keys.toList()[it]
                window.location.hash = "#"
            }

            val byPersonality = getCharacters().groupBy { it.snapshots.last().personalityFirst }.entries.sortedBy { it.key.name }.associate { (level, list) -> level.format() to list.size }
            chartTable("personality-chart", byPersonality, listOf("Personality", "Count"), "Characters by Top Personality") {
                searchOptions.searchText = byPersonality.keys.toList()[it]
                window.location.hash = "#"
            }

            val byPersonalitySecond = getCharacters().groupBy { it.snapshots.last().personalitySecond }.entries.sortedBy { it.key.name }.associate { (level, list) -> level.format() to list.size }
            chartTable("personality-second-chart", byPersonalitySecond, listOf("Personality", "Count"), "Characters by Second Personality") {
                searchOptions.searchText = byPersonalitySecond.keys.toList()[it]
                window.location.hash = "#"
            }

            val byTheme = mutableMapOf<String, Pair<String, Int>>()
            getCharacters().forEach { char ->
                char.snapshots.last().themes.forEach { theme ->
                    byTheme[theme.id] = Pair(theme.name, (byTheme[theme.id]?.second ?: 0) + 1)
                }
            }
            val byThemeSorted = byTheme.entries.sortedBy { it.value.first }.associate { (key, value) -> key to value }
            val byThemeName = byThemeSorted.map { (_, nameCount) -> nameCount.first to nameCount.second }.toMap()
            chartTable("theme-chart", byThemeName, listOf("Theme", "Count"), "Characters by Theme") {
                searchOptions.searchText = byThemeSorted.keys.toList()[it]
                window.location.hash = "#"
            }

            val popularity = getCharacters().map { it to it.friendships.size }.filter { it.second > 1 }.sortedByDescending { it.second }.take(20).toMap()
            chartTable("popularity-chart", popularity, listOf("Character", "Friend Count"), "Popularity (Relationship Count)")

            val campaigns = getCharacters().map { it to it.companyIds.size }.filter { it.second > 1 }.sortedByDescending { it.second }.take(20).toMap()
            chartTable("campaigns-chart", campaigns, listOf("Character", "Campaign Count"), "Campaigns Participated In")

            val kills = getCharacters().map { it to it.killCount }.filter { it.second > 10 }.sortedByDescending { it.second }.take(20).toMap()
            chartTable("kills-chart", kills, listOf("Character", "Kills"), "Confirmed Kills")

            val enemyTypes = getCompanies().groupBy {
                it.mainThreat
            }.map { (group, campaigns) -> Pair<String, String?>(group.capitalize(), "images/foe/$group.png") to campaigns.size }.sortedBy { it.first.first }.toMap()
            chartTableWithPic("enemy-campaign-count", enemyTypes, listOf("Group", "Runs"), "Campaigns Against Enemy")

            val enemyKills = listOfNotNull(
                profile.unlocks.firstOrNull { it.id == "achievementProgress_cultistKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_drauvenKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_gorgonKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_morthagiKills" },
                profile.unlocks.firstOrNull { it.id == "achievementProgress_thrixlKills" },
            ).map { it.name.replace("Kills", "").trim() to it.progress }
                .sortedBy { it.first }
                .associate { (key, value) -> Pair<String, String?>(key, "images/foe/${key.lowercase()}.png") to value }
            chartTableWithPic("enemy-kill-count", enemyKills, listOf("Group", "Kills"), "Enemies Killed")

        }
    }
}

private fun skillTable(parent: HTMLElement, stat: Stat) {
    el<HTMLElement?>("skill-table-section")?.remove()
    parent.append {
        div("profile-charts") {
            div {
                id = "skill-table-section"
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
                chartTable("best-stat-chart", bestStat, listOf("Character", stat.format()), "Highest Starting ${stat.format()}")
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.chartTable(docId: String, data: Map<LegacyCharacter, Number>, headers: List<String>, caption: String) {
    val namedData = data.mapKeys { (character, _) -> character.snapshots.last().name to getPicture("${character.uuid}/head") }
    chartTableWithPic(docId, namedData, headers, caption) { characterDetail(data.keys.toList()[it]) }
}

private fun TagConsumer<HTMLElement>.chartTable(docId: String, data: Map<String, Number>, headers: List<String>, caption: String, onClick: (Int) -> Unit = {}) {
    chartTableWithPic(docId, data.mapKeys { (key, _) -> key to null }, headers, caption, onClick)
}

private fun TagConsumer<HTMLElement>.chartTableWithPic(docId: String, data: Map<Pair<String, String?>, Number>, headers: List<String>, caption: String, onClick: (Int) -> Unit = {}) {
    val max = data.values.map { it.toFloat() }.maxOfOrNull { it }
    if (max != null) {
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
                    data.entries.forEachIndexed { i, (pair, count) ->
                        val (key, url) = pair
                        tr {
                            th(ThScope.row, classes = "profile-header") {
                                if (url != null) {
                                    img(classes = "label-image") {
                                        src = url
                                    }
                                }
                                +key
                            }
                            td {
                                style = "--size: calc( $count / $max )"
                                +"$count"
                            }
                            onClickFunction = { onClick(i) }
                        }
                    }
                }
            }
        }
    }
}