package pages

import Profile
import clearSections
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
        buildUnlocks(profile)
        buildCompanies()
    }
}

private fun TagConsumer<HTMLElement>.buildProfileNav() {
    div {
        id = "profile-nav"
        button {
            +"Back"
            onClickFunction = {
                window.location.hash = "#"
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.buildLinks(profile: Profile) {
    div(classes = "profile-section") {
        id = "profile-links"
        h2 { +"Links" }
        ol {
            li {
                a("https://wildermyth.com/wiki/Relationship") {
                    +"Wiki"
                    target = "_blank"
                }
            }
            li {
                a("https://steamcommunity.com/stats/763890/achievements/") {
                    +"Achievements"
                    target = "_blank"
                }
            }
            li {
                a("https://steamcommunity.com/id/${profile.name}/stats/763890/achievements/") {
                    +"Personal Achievements (Guess)"
                    target = "_blank"
                }
            }
            li {
                a("https://steamcommunity.com/app/763890/workshop/") {
                    +"Mods"
                    target = "_blank"
                }
            }
            li {
                a("https://github.com/ManApart/wildermyth-legacy") {
                    +"Site Source"
                    target = "_blank"
                }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.buildUnlocks(profile: Profile) {
    div(classes = "profile-section") {
        id = "profile-unlocks"
        h2 { +"Achievements" }
        table {
            tr {
                th { +"Achievement" }
                th { +"Id" }
                th { +"Count" }
            }
            tbody {
                profile.unlocks.forEach { unlock ->
                    val achievementName = unlock.id
                    val progress = if (unlock.progress == 0) "" else unlock.progress.toString()
                    tr {
                        td { +unlock.id }
                        td { +achievementName}
                        td { +progress }
                    }
                }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.buildCompanies() {
    div(classes = "profile-section") {
        id = "profile-companies"
        h2 { +"Companies" }
        getCompanies().forEach { company ->
            companyCard(company)
        }
    }
}