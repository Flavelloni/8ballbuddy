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
)
