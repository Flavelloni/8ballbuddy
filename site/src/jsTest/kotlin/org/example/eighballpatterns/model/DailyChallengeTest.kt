package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyChallengeTest {
    @Test
    fun progressAndScoreFollowTheServerSuppliedRound() {
        var attempt = DailyChallengeAttempt(DailyChallenge("2026-10-16", listOf("a", "b")))
        assertEquals("a", attempt.currentShotId)
        attempt = attempt.copy(answers = listOf(ChallengeAnswer("a", 6, 6, 1500)))
        assertFalse(attempt.complete)
        assertEquals("b", attempt.currentShotId)
        attempt = attempt.copy(answers = attempt.answers + ChallengeAnswer("b", 1, 6, 2500))
        assertTrue(attempt.complete)
        assertEquals(null, attempt.currentShotId)
        assertEquals(1, attempt.correctCount)
        assertEquals(4000, attempt.totalElapsedMs)
        assertEquals(0, attempt.currentShotElapsedMs)
    }

    @Test
    fun clockCountsOnlyRunningTimeAndResumesSavedTime() {
        var now = 1000.0
        val clock = AnswerClock({ now }, initialElapsedMs = 2000)
        now += 5000
        assertEquals(2000, clock.elapsedMs)
        clock.start()
        clock.start()
        now += 1250
        assertEquals(3250, clock.elapsedMs)
        clock.pause()
        now += 10000
        clock.pause()
        assertEquals(3250, clock.elapsedMs)
        clock.start()
        now += 750
        clock.pause()
        assertEquals(4000, clock.elapsedMs)
    }

    @Test
    fun formatsTenthsAcrossMinuteBoundary() {
        assertEquals("0:00.0", formatChallengeTime(0))
        assertEquals("0:59.9", formatChallengeTime(59999))
        assertEquals("1:00.0", formatChallengeTime(60000))
        assertEquals("10:01.2", formatChallengeTime(601200))
    }

    @Test
    fun welcomeLeadsToChoiceAndBrowsingRemainsReversible() {
        assertEquals(ReelPosition(ReelStage.ModeChoice), ReelPosition().next(3))
        assertEquals(ReelPosition(), ReelPosition(ReelStage.ModeChoice).previous())
        assertEquals(ReelPosition(ReelStage.ModeChoice), ReelPosition(ReelStage.Player).previous())
        assertEquals(ReelPosition(ReelStage.Player), ReelPosition(ReelStage.Shot, 2).next(3))
    }
}
