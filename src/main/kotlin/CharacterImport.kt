import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.files.get

fun importMenu() {
    document.body!!.append.div {
        fileInput {
            id = "importInput"
            type = InputType.file
            onChangeFunction = {
                val element = document.getElementById(id) as HTMLInputElement
                if (element.files != undefined) {
                    val reader = FileReader()
                    reader.onload = {
                        importZip(reader.result as ArrayBuffer)
                    }
                    reader.onerror = { error ->
                        console.error("Failed to read File $error")
                    }
                    reader.readAsArrayBuffer(element.files!![0]!!)
                }
            }
        }
    }
}

fun importZip(data: ArrayBuffer) {
    JSZip().loadAsync(data).then { zip ->
        val keys = JsonObject.keys(zip.files)
        handleZipCharacterData(zip, keys)
        handleZipPictures(zip, keys)
    }
}

private fun handleZipCharacterData(zip: JSZip.ZipObject, keys: List<String>) {
    keys.filter { fileName ->
        fileName.endsWith("data.json")
    }.forEach { fileName ->
        zip.file(fileName).async<String>("string").then { contents ->
            println(fileName)
        }
    }
}

private fun handleZipPictures(zip: JSZip.ZipObject, keys: List<String>) {
    keys.filter { fileName ->
        fileName.endsWith("default.png") || fileName.endsWith("body.png")
    }.forEach { fileName ->
        println(fileName)
        zip.file(fileName).async<Blob>("Blob").then { contents ->
            println(contents)
            document.body?.append {
                img {
                    src = URL.Companion.createObjectURL(contents)
                }
            }
        }
    }
}