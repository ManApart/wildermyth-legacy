import kotlinx.browser.localStorage
import kotlinx.serialization.encodeToString
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.xhr.BLOB
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.js.Json

fun loadExample() {
    val json = JSON.parse<Json>(defaultData)
    val example = parseFromJson(json)
//    println(JSON.stringify(example))
    println(kotlinx.serialization.json.Json.encodeToString(example))
    localStorage[example.uuid] = kotlinx.serialization.json.Json.encodeToString(example)
    val characters = getCharacterList()
    characters.add(example.uuid)
    saveCharacterList(characters)

    loadBlob("./example/body.png") {
        savePicture(example.uuid + "/body", it)
    }
    loadBlob("./example/default.png") {
        savePicture(example.uuid + "/head", it)
    }

}

private fun loadBlob(url: String, callBack: (Blob) -> Unit) {
    XMLHttpRequest().apply {
        open("GET", url)
        responseType = XMLHttpRequestResponseType.BLOB
        onerror = { println("Failed to get image") }
        onload = {
            callBack(response as Blob)
        }
        send()
    }
}