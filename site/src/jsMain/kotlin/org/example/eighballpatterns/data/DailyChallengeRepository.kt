package org.example.eighballpatterns.data

import kotlinx.browser.window
import org.example.eighballpatterns.model.ChallengeAnswer
import org.example.eighballpatterns.model.DailyChallenge
import org.example.eighballpatterns.model.DailyChallengeAttempt
import org.example.eighballpatterns.model.DAILY_CHALLENGE_SHOT_LIMIT
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.createDailyChallenge
import org.example.eighballpatterns.model.eightBallReelPickerBalls

internal fun currentChallengeDate(): String = js("new Date().toISOString().slice(0, 10)") as String

private fun challengeStorageKey(date: String) = "eight-ball-daily-v1-$date"

internal fun loadDailyChallenge(date: String, shots: List<EightBallShot>): DailyChallengeAttempt {
    val fallback = DailyChallengeAttempt(createDailyChallenge(date, shots))
    return try {
        val text = window.localStorage.getItem(challengeStorageKey(date)) ?: return fallback
        val item = JSON.parse<dynamic>(text)
        require(item.version == 1 && item.date == date)
        val ids = (item.shotIds as Array<String>).toList()
        require(ids.isNotEmpty() && ids.size <= DAILY_CHALLENGE_SHOT_LIMIT && ids.distinct().size == ids.size)
        val available = shots.associateBy { it.entry.id }
        require(ids.all { it in available })
        val answers = (item.answers as Array<dynamic>).mapIndexed { index, answer ->
            val shot = available.getValue(ids[index])
            val selected = answer.selectedBall as Int
            val elapsed = answer.elapsedMs as Int
            require(answer.shotId == shot.entry.id && selected in eightBallReelPickerBalls(shot) && elapsed >= 0)
            ChallengeAnswer(shot.entry.id, selected, shot.meta.ball, elapsed)
        }
        val elapsed = item.currentShotElapsedMs as Int
        require(elapsed >= 0)
        DailyChallengeAttempt(DailyChallenge(date, ids), answers, elapsed)
    } catch (_: Throwable) {
        fallback
    }
}

internal fun saveDailyChallenge(attempt: DailyChallengeAttempt): Boolean = try {
    val item = js("({})")
    item.version = 1
    item.date = attempt.challenge.date
    item.shotIds = attempt.challenge.shotIds.toTypedArray()
    item.currentShotElapsedMs = attempt.currentShotElapsedMs
    item.answers = attempt.answers.map { answer ->
        val record = js("({})")
        record.shotId = answer.shotId
        record.selectedBall = answer.selectedBall
        record.elapsedMs = answer.elapsedMs
        record
    }.toTypedArray()
    window.localStorage.setItem(challengeStorageKey(attempt.challenge.date), JSON.stringify(item))
    true
} catch (_: Throwable) {
    false
}
