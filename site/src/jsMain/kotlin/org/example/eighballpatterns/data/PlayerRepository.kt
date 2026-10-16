package org.example.eighballpatterns.data

import com.varabyte.kobweb.navigation.BasePath
import org.example.eighballpatterns.model.PlayerProfile

internal fun loadPlayerProfiles(onLoaded: (Map<String, PlayerProfile>) -> Unit) {
    fetchResourceText(
        BasePath.prependTo("/players.json"),
        onSuccess = { text -> onLoaded(parsePlayerProfiles(text)) },
        onError = { onLoaded(emptyMap()) },
    )
}

private fun parsePlayerProfiles(text: String): Map<String, PlayerProfile> {
    val players = js("JSON.parse(text)")
    val names = js("Object.keys(players)") as Array<String>
    return names.associate { name ->
        val item = players[name]
        name.trim().lowercase() to PlayerProfile(
            name = name,
            imageUrl = (item.imageUrl as? String).orEmpty(),
            imageCredit = (item.imageCredit as? String).orEmpty(),
            imageSourceUrl = (item.imageSourceUrl as? String).orEmpty(),
            imagePosition = (item.imagePosition as? String) ?: "center",
        )
    }
}
