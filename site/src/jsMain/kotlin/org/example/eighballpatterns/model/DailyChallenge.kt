package org.example.eighballpatterns.model

internal const val DAILY_CHALLENGE_SHOT_LIMIT = 10

internal data class DailyChallenge(val date: String, val shotIds: List<String>)

internal data class ChallengeAnswer(
    val shotId: String,
    val selectedBall: Int,
    val correctBall: Int,
    val elapsedMs: Int,
) {
    val correct: Boolean get() = selectedBall == correctBall
}

internal data class DailyChallengeAttempt(
    val challenge: DailyChallenge,
    val answers: List<ChallengeAnswer> = emptyList(),
    val currentShotElapsedMs: Int = 0,
) {
    val complete: Boolean get() = answers.size == challenge.shotIds.size
    val currentShotId: String? get() = challenge.shotIds.getOrNull(answers.size)
    val correctCount: Int get() = answers.count { it.correct }
    val totalElapsedMs: Int get() = answers.sumOf { it.elapsedMs }

    fun answer(shot: EightBallShot, selectedBall: Int, elapsedMs: Int): DailyChallengeAttempt {
        require(shot.entry.id == currentShotId)
        require(selectedBall in eightBallReelPickerBalls(shot))
        return copy(
            answers = answers + ChallengeAnswer(shot.entry.id, selectedBall, shot.meta.ball, elapsedMs.coerceAtLeast(0)),
            currentShotElapsedMs = 0,
        )
    }
}

internal fun createDailyChallenge(date: String, shots: List<EightBallShot>): DailyChallenge {
    // A stable per-date ordering keeps the round identical across browsers and input order.
    val ids = shots.filter { it.meta.ball in 1..15 && it.meta.ball in eightBallReelPickerBalls(it) }
        .map { it.entry.id }.distinct()
        .sortedWith(compareBy<String> { id ->
            "$date/$id".fold(0x811c9dc5.toInt()) { hash, char -> (hash xor char.code) * 16777619 }.toUInt()
        }.thenBy { it })
        .take(DAILY_CHALLENGE_SHOT_LIMIT)
    return DailyChallenge(date, ids)
}

internal fun formatChallengeTime(elapsedMs: Int): String {
    val tenths = elapsedMs.coerceAtLeast(0) / 100
    return "${tenths / 600}:${(tenths / 10 % 60).toString().padStart(2, '0')}.${tenths % 10}"
}

internal class AnswerClock(private val now: () -> Double, initialElapsedMs: Int = 0, val continuous: Boolean = false) {
    private var accumulatedMs = initialElapsedMs.toDouble()
    private var startedAt: Double? = null

    val elapsedMs: Int get() = (accumulatedMs + (startedAt?.let { now() - it } ?: 0.0)).toInt().coerceAtLeast(0)

    fun start() {
        if (startedAt == null) startedAt = now()
    }

    fun pause() {
        startedAt?.let { accumulatedMs += now() - it }
        startedAt = null
    }
}
