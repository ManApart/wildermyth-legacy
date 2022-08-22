import kotlin.js.Json

data class Character(val name: String) {
    val fileName = name.replace(Regex.fromLiteral("[^a-zA-Z\\d\\s:]"), "").trim()
}


fun parseFromJson(json: Json): Character {
    val base = (json["entities"] as Array<Array<Json>>)[0][2]
    val name = base["name"] as String

//    println(JSON.stringify(base))
    return Character(name)
}