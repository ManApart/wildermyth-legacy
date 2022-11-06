import pages.filterCharacterDoms

var previousSearch: CharacterSearchOptions? = null

fun characterSearch() {
    val info = getAdditionalInfo()
    val characters = getCharacters()
        .filterFavorites(searchOptions.favoritesOnly, info)
        .hideNPC(searchOptions.hideNPC)
        .filterSearch(searchOptions.searchText, info)
    filterCharacterDoms(characters)
    saveSearch(searchOptions)
    previousSearch = searchOptions.copy()
}

private fun List<LegacyCharacter>.filterFavorites(doFilter: Boolean, info: MutableMap<String, AdditionalInfo>): List<LegacyCharacter> {
    return if (doFilter) filter { info[it.uuid]?.favorite ?: false } else this
}

private fun List<LegacyCharacter>.hideNPC(doFilter: Boolean): List<LegacyCharacter> {
    return if (doFilter) filter { !it.npc } else this
}

private fun List<LegacyCharacter>.filterSearch(searchText: String, info: Map<String, AdditionalInfo>): List<LegacyCharacter> {
    return if (searchText.isBlank()) this else {
        searchText.lowercase().split(",").fold(this) { acc, s -> filterCharacters(acc, s, info) }
    }
}

private fun filterCharacters(initial: List<LegacyCharacter>, searchText: String, info: Map<String, AdditionalInfo>): List<LegacyCharacter> {
    return initial.filter { character ->
        characterFilter(character, searchText)
                || additionalInfoFilter(info[character.uuid], searchText)
                || latestFilter(character.snapshots.last(), searchText)
                || snapshotsFilter(character.snapshots, searchText)
    }
}


private fun characterFilter(character: LegacyCharacter, searchText: String): Boolean {
    return character.legacyTierLevel.format().lowercase().contains(searchText)
}

private fun additionalInfoFilter(info: AdditionalInfo?, searchText: String): Boolean {
    return info?.tags?.any { it.contains(searchText) } ?: false
}

private fun snapshotsFilter(snapshots: Array<Character>, searchText: String): Boolean {
    return snapshots.any { snapshot ->
        snapshot.name.lowercase().contains(searchText)
                || snapshot.aspects.any { it.name.lowercase().contains(searchText) }
    }
}

private fun latestFilter(latest: Character, searchText: String): Boolean {
    return latest.classLevel.takeIf { it != undefined }?.name?.lowercase()?.contains(searchText) ?: false ||
            latest.personalityFirst.name.lowercase().contains(searchText) ||
            latest.personalitySecond.name.lowercase().contains(searchText)
}
