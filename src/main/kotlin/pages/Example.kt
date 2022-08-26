package pages

import defaultData
import getCharacterList
import jsonMapper
import kotlinx.browser.localStorage
import kotlinx.html.InputType
import kotlinx.serialization.encodeToString
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.xhr.BLOB
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import saveCharacterList
import savePicture
import kotlin.js.Json
import kotlin.js.Promise

fun loadExample() {
    val json = JSON.parse<Json>(defaultData)
    val example = parseFromJson(json)
    localStorage[example.uuid] = jsonMapper.encodeToString(example)
    val characters = getCharacterList()
    characters.add(example.uuid)
    saveCharacterList(characters)

    Promise.all(
        arrayOf(
            loadBlob("example/body.png"),
            loadBlob("example/default.png")
        )
    ).then { blobs ->
        Promise.all(
            arrayOf(
                savePicture(example.uuid + "/body", blobs.first()),
                savePicture(example.uuid + "/head", blobs.last())
            )
        ).then {
            displayCharacters()
        }
    }
}

private fun loadBlob(url: String): Promise<Blob> {
    return Promise { resolve, reject ->
        XMLHttpRequest().apply {
            open("GET", url)
            responseType = XMLHttpRequestResponseType.BLOB
            onerror = { println("Failed to get image") }
            onload = {
                resolve(response as Blob)
            }
            send()
        }
    }

}

//private fun loadBlob(url: String, callBack: (Blob) -> Unit) {
//    XMLHttpRequest().apply {
//        open("GET", url)
//        responseType = XMLHttpRequestResponseType.BLOB
//        onerror = { println("Failed to get image") }
//        onload = {
//            callBack(response as Blob)
//        }
//        send()
//    }
//
//}