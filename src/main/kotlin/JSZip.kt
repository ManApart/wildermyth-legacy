import org.khronos.webgl.ArrayBuffer
import kotlin.js.Json
import kotlin.js.Promise

@JsModule("jszip")
@JsNonModule
external class JSZip {
    fun loadAsync(content: ArrayBuffer) : Promise<ZipObject>
}

external class ZipObject {
//    val name: String
//    fun file(fileName: String)
    val files: Json
}

external class ZipFile {
    val filename: String
    val file: Any
}

