import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.KeyboardEvent
import pages.*
import kotlin.js.Date

val jsonMapper = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
lateinit var favicon: HTMLElement
var searchOptions = CharacterSearchOptions()

fun main() {
    window.onload = {
        favicon = document.getElementById("favicon") as HTMLElement
        createDB()
        loadExample()
    }
    window.addEventListener("popstate", { e ->
        doRouting()
    })

    window.addEventListener("keyup", { event ->
        val key = (event as KeyboardEvent)
        if (document.activeElement !is HTMLTextAreaElement || document.activeElement !is HTMLInputElement) {
            onKeyUp(key)
        }
    })
    window.addEventListener("keydown", { event ->
        val key = (event as KeyboardEvent)
        if (document.activeElement !is HTMLTextAreaElement || document.activeElement !is HTMLInputElement) {
            onKeyDown(key)
        }
    })
}

fun doRouting() {
    doRouting(window.location.hash)
}

fun doRouting(windowHash: String) {
    when {
        windowHash.startsWith("#profile") -> {
            profile()
        }

        windowHash.startsWith("#detail/") -> {
            val hash = windowHash.replace("#detail/", "")
            getCharacter(hash)?.let { character ->
                characterDetail(character)
            } ?: displayCharacters()
        }

        windowHash.startsWith("#network/") -> {
            val hash = windowHash.replace("#network/", "")
            getCharacter(hash)?.let { character ->
                buildRelationshipNetwork(character)
            } ?: displayCharacters()
        }

        windowHash.startsWith("#family/") -> {
            val hash = windowHash.replace("#family/", "")
            getCharacter(hash)?.let { character ->
                buildRelationshipNetwork(character, true)
            } ?: displayCharacters()
        }

        else -> {
            displayCharacters()
            characterSearch()
        }
    }
}

fun clearSections() {
    el("character-cards-section").innerHTML = ""
    el("import-section").innerHTML = ""
    el("profile-section").innerHTML = ""
    el("character-detail-section").innerHTML = ""
    el("relationship-network-section").innerHTML = ""
    el("nav").innerHTML = ""
}

fun el(id: String) = document.getElementById(id) as HTMLElement
fun <T> el(id: String) = document.getElementById(id) as T

fun Enum<*>.format(): String {
    return if (this === undefined) "" else name.split("_").joinToString(" ") { it.lowercase().capitalize() }
}

fun String.format(): String {
    return lowercase().capitalize()
}

private val capitalSplitRegex = "(?=\\p{Upper})".toRegex()
fun String.splitByCapitals(capitalize: Boolean = false): String {
    return if (capitalize) {
        split(capitalSplitRegex).flatMap { it.split(" ") }.joinToString(" ") { it.capitalize() }
    } else {
        split(capitalSplitRegex).joinToString(" ")
    }
}

fun String.removeAll(vararg parts: String): String {
    return parts.fold(this) { acc, s -> acc.replace(s, "") }
}

fun isMobile(): Boolean {
    return window.screen.width / window.screen.height < 3 / 4f
}

private var logTime = Date().getMilliseconds()

fun logStart(message: String) {
    val now = Date().getMilliseconds()
    logTime = now
    println(message)
}

fun log(message: String) {
    val now = Date().getMilliseconds()
    val elapsed = now - logTime
    logTime = now
    println("$elapsed: $message")
}