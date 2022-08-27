package pages

import AdditionalInfo
import jsonMapper
import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.input
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import saveAdditionalInfo
import kotlinx.serialization.decodeFromString

fun importMenu() {
    val section = document.getElementById("import-section")!!
    section.innerHTML = ""
    document.getElementById("character-detail-section")!!.innerHTML = ""
    section.append {
        div {
            id = "upload-instructions"
            p { +"View your own characters from Wildermyth!" }
            ol {
                li { +"In game, go to My Legacy -> Heroes -> Hero List." }
                li {
                    +"For each hero you want to view."
                    ul {
                        li { +"Click their picture to view their character sheet." }
                        li { +"Click 'Customize' (or press 7)" }
                        li { +"Click 'Export Character'" }
                        li { +"This should open a new window, which you can ignore until you've exported each character." }
                        li { +"Click the change hero arrow abover their name to go to the next hero." }
                    }
                }
                li { +"Navigate to your '<Game Install>/out' folder." }
                ul {
                    li { +"If you just exported characters, it should be one folder up from the character export." }
                    li { +"Otherwise it should be 'steamapps/common/Wildermyth/out' or the GOG equivalent." }
                }
                li { +"Select all the character folders and create a zip file." }
                ul {
                    li { a("https://www.7-zip.org/", target = "_blank") { +"7Zip is a good tool for this." } }
//                    li { +"${a("https://www.7-zip.org/", target = "_blank") { +"7Zip" }} is a good tool for this." }
                }
                li { +"Upload the zip using the button below. Your characters should all load and be locally saved!" }
            }
        }
        button {
            +"Cancel"
            onClickFunction = { section.innerHTML = "" }
        }
        input(InputType.file) {
            id = "import-input"
            type = InputType.file
            onChangeFunction = {
                val element = document.getElementById(id) as HTMLInputElement
                if (element.files != undefined) {
                    val file = element.files!![0]!!
                    val reader = FileReader()
                    reader.onload = {
                        if (file.name == "AdditionalInfo.json") {
                            importAdditionalInfo(reader.result as String)
                        } else {
                            importZip(reader.result as ArrayBuffer)
                        }
                    }
                    reader.onerror = { error ->
                        console.error("Failed to read File $error")
                    }
                    if (file.name == "AdditionalInfo.json") {
                        reader.readAsText(file)
                    } else {
                        reader.readAsArrayBuffer(file)
                    }
                }
            }
        }
    }
}

fun importAdditionalInfo(content: String) {
    saveAdditionalInfo(jsonMapper.decodeFromString<MutableMap<String, AdditionalInfo>>(content))
    displayCharacters()
}