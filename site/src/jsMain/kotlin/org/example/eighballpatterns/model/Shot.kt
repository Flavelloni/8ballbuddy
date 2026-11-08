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
    val archived: Boolean = false,
    val ballColors: Map<Int, Int> = emptyMap(),
    val browseOrder: Int = 0,
    val ballInHand: Boolean = false,
    val matchYear: Int? = null,
) {
    // Normal answers use 0 for no pot; breaks use 0..4 for the count potted.
    val correctAnswer: Int get() = if (isBreak) ballsPotted ?: -1 else ball
}

internal data class EightBallShot(
    val entry: EightBallEntry,
    val meta: EightBallMeta,
)

internal fun browseEightBallShots(shots: List<EightBallShot>): List<EightBallShot> =
    shots.filterNot { it.meta.archived }
        .sortedWith(compareBy<EightBallShot> { it.meta.browseOrder }.thenBy { it.entry.id })
