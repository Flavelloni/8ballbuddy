package org.example.eighballpatterns.model

import kotlin.random.Random

internal data class EightBallReelStats(
    val answered: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
) {
    fun next(wasCorrect: Boolean): EightBallReelStats {
        val nextStreak = if (wasCorrect) streak + 1 else 0
        return copy(
            answered = answered + 1,
            correct = correct + if (wasCorrect) 1 else 0,
            streak = nextStreak,
            bestStreak = maxOf(bestStreak, nextStreak),
        )
    }
}

private val positiveEightBallFeedback = listOf(
    "That's it!",
    "Exactly!",
    "Nice read.",
    "Right ball.",
    "You called it.",
    "Sharp pick.",
)

private val negativeEightBallFeedback = listOf(
    "Not this time.",
    "Different ball.",
    "Good try.",
    "Nope, another ball.",
    "The table had other plans."
)

internal fun buildEightBallReelFeedback(correct: Boolean): String {
    return if (correct) {
        positiveEightBallFeedback[Random.nextInt(positiveEightBallFeedback.size)]
    } else {
        negativeEightBallFeedback[Random.nextInt(negativeEightBallFeedback.size)]
    }
}

internal fun eightBallReelResultText(selectedBall: Int?, actualBall: Int): String {
    val actual = "${ballLabel(actualBall)} ball"
    return if (selectedBall == actualBall) {
        "Correct: $actual"
    } else {
        "You picked ${ballLabel(selectedBall ?: 0)}. Correct: $actual"
    }
}

internal fun eightBallReelPickerBalls(shot: EightBallShot): List<Int> {
    return when (shot.meta.group) {
        1 -> (1..7).toList()
        2 -> (9..15).toList()
        0 -> (1..15).filter { it != 8 }
        else -> (8..10).toList()
    }
}

internal fun eightBallReelTitle(meta: EightBallMeta): String {
    return when (meta.group) {
        0 -> "${meta.player} has an open table"
        1 -> "${meta.player} has solids"
        2 -> "${meta.player} has stripes"
        else -> "${meta.player} has group ${meta.group}"
    }
}

internal fun ballLabel(ball: Int): String {
    return if (ball in 1..15) ball.toString() else "unknown"
}
