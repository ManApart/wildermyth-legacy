package pages

import doRouting
import getCharacterList
import kotlinx.browser.window
import loadMemory
import org.w3c.files.Blob
import org.w3c.xhr.BLOB
import org.w3c.xhr.JSON
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import saveCharacter
import saveCharacterList
import savePicture
import kotlin.js.Json
import kotlin.js.Promise

private const val loadZip = true

fun loadExample() {
    val windowHash = window.location.hash
    loadJson("example/data.json").then { json ->
        val example = parseLegacyCharacter(json)
        saveCharacter(example)
        val characters = getCharacterList()
        characters.add(example.uuid)
        saveCharacterList(characters)

        Promise.all(
            arrayOf(
                loadBlob("example/body.png").then {
                    savePicture(example.uuid + "/body", it)
                },
                loadBlob("example/default.png").then {
                    savePicture(example.uuid + "/head", it)
                }
            )
        ).then {
            doRouting(windowHash)
            if (loadZip) loadZipIfPresent(windowHash)
        }
    }
}

private fun loadBlob(url: String): Promise<Blob> {
    return Promise { resolve, _ ->
        XMLHttpRequest().apply {
            open("GET", url)
            responseType = XMLHttpRequestResponseType.BLOB
            onerror = { println("Failed to get blob") }
            onload = {
                resolve(response as Blob)
            }
            send()
        }
    }
}

private fun loadJson(url: String): Promise<Json> {
    return Promise { resolve, _ ->
        XMLHttpRequest().apply {
            open("GET", url)
            responseType = XMLHttpRequestResponseType.JSON
            onerror = { println("Failed to get Json") }
            onload = {
                resolve(response as Json)
            }
            send()
        }
    }
}

private fun loadZipIfPresent(originalHash: String) {
    loadMemory().then {
        doRouting(originalHash)
    }
}