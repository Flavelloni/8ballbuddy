package org.example.eighballpatterns.model

internal data class EightBallEntry(
    val id: String,
    val metaPath: String,
    val imagePath: String,
    val videoPath: String,
)

internal data class EightBallMeta(
    val player: String,
    val opponent: String,
    val group: Int,
    val ball: Int,
    val comments: String,
)

internal data class EightBallShot(
    val entry: EightBallEntry,
    val meta: EightBallMeta,
)
