package pages

import Profile
import Stat
import Unlock
import clearSections
import el
import getCharacters
import getCompanies
import getProfile
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement

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
    buildSmartCharts()
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

