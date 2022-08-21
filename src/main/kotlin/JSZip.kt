import org.khronos.webgl.ArrayBuffer
import kotlin.js.Json
import kotlin.js.Promise

@JsModule("jszip")
@JsNonModule
external class JSZip {
    fun loadAsync(content: ArrayBuffer): Promise<ZipObject>
//    fun ZipObject(name: String, data: Any?, options: Any?)
    class ZipObject {
        val files: Json
        fun file(name: String): ZipObject
        fun <T> async(kind: String): Promise<T>
    }
}


//@JsModule("jszip")
//@JsNonModule
//external class ZipEntry {
//    fun async(kind: String): Promise<Any>
//}

object JsonObject {
    fun keys(obj: Any): Array<*> {
        return js("Object.keys(obj)") as Array<*>
    }

    fun entries(obj: Any): List<Pair<Any, Any>> {
        val raw = js("Object.entries(obj)") as Array<Array<Any>>
        return raw.map { it[0] to it[1] }
    }
}