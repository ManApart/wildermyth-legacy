package pages

import defaultData
import doRouting
import getCharacterList
import jsonMapper
import kotlinx.browser.localStorage
import kotlinx.html.InputType
import kotlinx.serialization.encodeToString
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.xhr.BLOB
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import saveCharacter
import saveCharacterList
import savePicture
import kotlin.js.Json
import kotlin.js.Promise

fun loadExample() {
    val json = JSON.parse<Json>(defaultData)
    val example = parseFromJson(json)
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
        doRouting()
        loadZipIfPresent()
    }
}

private fun loadBlob(url: String): Promise<Blob> {
    return Promise { resolve, reject ->
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

private fun loadZipIfPresent() {
    loadBlob("characters.zip").then { blob ->
        if (blob.size.toInt() > 200) {
            println("Found Zip")
            val reader = FileReader()
            reader.onload = {
                importZip(reader.result as ArrayBuffer)
            }
            reader.onerror = { error ->
                console.error("Failed to read File $error")
            }
            reader.readAsArrayBuffer(blob)
        }
    }
}