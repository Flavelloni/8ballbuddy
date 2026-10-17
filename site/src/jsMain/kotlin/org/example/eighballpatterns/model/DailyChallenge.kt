package org.example.eighballpatterns.model

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

}

internal fun formatChallengeTime(elapsedMs: Int): String {
    val tenths = elapsedMs.coerceAtLeast(0) / 100
    return "${tenths / 600}:${(tenths / 10 % 60).toString().padStart(2, '0')}.${tenths % 10}"
}

internal class AnswerClock(private val now: () -> Double, initialElapsedMs: Int = 0) {
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
