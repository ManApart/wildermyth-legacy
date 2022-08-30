
fun Character.interpolate(line: String): String {
    val (templates, parts) = identifyTemplates(line)
    println("templates: $templates")
    println("parts: $parts")
    val interpolated = templates.map { this.replaceTemplate(it) }

    return parts.indices.joinToString("") { i ->
        parts[i] + (interpolated.getOrNull(i) ?: "")
    }
}

private fun identifyTemplates(line: String): Pair<List<String>, List<String>>{
    val chunks = mutableListOf<Pair<Int, Int>>()
    var current = findToken(line, 0)
    while(current != null){
        chunks.add(current)
        current = findToken(line, chunks.last().second)
    }

    val parts = mutableListOf<String>()
    var i = 0
    val templates = chunks.map { (start, end) ->
        parts.add(line.substring(i, start))
        i = end
        line.substring(start+1, end-1)
    }
    parts.add(line.substring(i, line.length))

    return templates to parts
}

private fun findToken(line: String, from: Int): Pair<Int, Int>? {
    val start = line.indexOf("<", from)
    val end = line.indexOf(">", start)+1
    return if (start == -1 || end == 0) null else start to end
}

private fun Character.replaceTemplate(template: String): String {
    val parts = template.split(":")
    val type = parts.first()
    val typeOptions = type.split("/")
    val resultOptions = parts.last().split("/")
    return when {
        template == "name" -> name
        type == "mf" -> replaceMF(resultOptions)
        typeOptions.any { it in personalityNames } -> replacePersonality(typeOptions, resultOptions)
        else ->  {
            println("Unknown type: $type")
            resultOptions.last()
        }
    }
}

private fun Character.replaceMF(resultOptions: List<String>): String {
    return when {
        sex == Sex.MALE -> resultOptions.first()
        sex == Sex.FEMALE -> resultOptions[1]
        resultOptions.size == 3 -> resultOptions.last()
        else -> resultOptions.first()
    }
}

private fun Character.replacePersonality(typeOptions: List<String>, resultOptions: List<String>): String {
    val highest = typeOptions.maxByOrNull { personality[Personality.valueOf(it.uppercase())] ?: 0 } ?: typeOptions.first()
    val resultIndex = typeOptions.indexOf(highest)
    return resultOptions[resultIndex]
}