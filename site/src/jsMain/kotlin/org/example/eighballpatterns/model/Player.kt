package org.example.eighballpatterns.model

internal data class PlayerProfile(
    val name: String,
    val imageUrl: String,
    val imageCredit: String,
    val imageSourceUrl: String,
    val imagePosition: String,
    val fargoRating: Int? = null,
    val fargoSource: String = "",
    val fargoDate: String = "",
    val club: String = "",
    val country: String = "",
    val countryFlagUrl: String = "",
)

internal fun orderPlayerChoices(players: List<String>, profiles: Map<String, PlayerProfile>): List<String> =
    players.sortedBy { name ->
        if (profiles[name.trim().lowercase()]?.club?.trim().equals("OBK", ignoreCase = true)) 0 else 1
    }
