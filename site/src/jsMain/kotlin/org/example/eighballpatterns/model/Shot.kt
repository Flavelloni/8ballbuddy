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
    val source: String = "",
    val videoLink: String = "",
    val isBreak: Boolean = false,
    val ballsPotted: Int? = null,
) {
    // Keep numbered-ball answers separate from break counts (including zero).
    val correctAnswer: Int get() = if (isBreak) ballsPotted ?: -1 else ball
}

internal data class EightBallShot(
    val entry: EightBallEntry,
    val meta: EightBallMeta,
)
