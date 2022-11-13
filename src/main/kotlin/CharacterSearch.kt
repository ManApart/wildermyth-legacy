import pages.filterCharacterDoms

var previousSearch: CharacterSearchOptions? = null

fun characterSearch() {
    val info = getAdditionalInfo()
    val characters = getCharacters()
        .filterFavorites(searchOptions.favoritesOnly, info)
        .hideNPC(searchOptions.hideNPC)
        .filterSearch(searchOptions.searchText, info)
    filterCharacterDoms(characters)
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
            latest.personalitySecond.name.lowercase().contains(searchText) ||
            latest.gear.any { it.name.lowercase().contains(searchText) }
}


fun List<LegacyCharacter>.sorted(sort: CharacterSort, favoritesFirst: Boolean, info: Map<String, AdditionalInfo>): List<LegacyCharacter> {
    return when (sort) {
        CharacterSort.ALPHABETICAL -> sortedAlphabetical(info, favoritesFirst)
        CharacterSort.RANK -> sortedRank(info, favoritesFirst)
        CharacterSort.ACQUIRED -> sortedAcquired(info, favoritesFirst)
    }
}

private fun List<LegacyCharacter>.sortedAlphabetical(info: Map<String, AdditionalInfo>, favoritesFirst: Boolean): List<LegacyCharacter> {
    return if (favoritesFirst) {
        sortedWith(compareBy<LegacyCharacter> { !(info[it.uuid]?.favorite ?: false) }
            .thenBy { it.snapshots.last().name.split(" ").last() }
            .thenBy { it.snapshots.last().name.split(" ").first() })
    } else {
        sortedWith(compareBy<LegacyCharacter> { it.snapshots.last().name.split(" ").last() }
            .thenBy { it.snapshots.last().name.split(" ").first() })
    }
}

private fun List<LegacyCharacter>.sortedRank(info: Map<String, AdditionalInfo>, favoritesFirst: Boolean): List<LegacyCharacter> {
    return if (favoritesFirst) {
        sortedWith(compareBy<LegacyCharacter> { !(info[it.uuid]?.favorite ?: false) }
            .thenByDescending { it.legacyTierLevel })
    } else {
        sortedWith(compareByDescending { it.legacyTierLevel })
    }
}

private fun List<LegacyCharacter>.sortedAcquired(info: Map<String, AdditionalInfo>, favoritesFirst: Boolean): List<LegacyCharacter> {
    return if (favoritesFirst) {
        sortedWith(compareBy<LegacyCharacter> { !(info[it.uuid]?.favorite ?: false) }
            .thenBy { it.snapshots.first().date })
    } else {
        sortedWith(compareBy { it.snapshots.first().date })
    }

}