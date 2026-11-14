package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyChallengeTest {
    @Test fun groupsAdvanceDirectlyToThePlayerAndRemainReversibleAcrossTheFeedLoop() {
        val firstIntro = ReelPosition(ReelStage.Player)
        assertEquals(ReelPosition(ReelStage.ModeChoice), firstIntro.previous())
        assertEquals(ReelPosition(ReelStage.Shot), firstIntro.next(3))
        val end = ReelPosition(ReelStage.Shot, 1)
        val intro = end.next(3, nextStartsGroup = true)
        assertEquals(ReelPosition(ReelStage.Player, 2), intro)
        assertEquals(ReelPosition(ReelStage.Shot, 2), intro.next(3))
        assertEquals(end, intro.previous(shotCount = 3))
        assertEquals(ReelPosition(ReelStage.Shot, 2), end.next(3, nextStartsGroup = false))
        assertEquals(ReelPosition(ReelStage.Player, 2), end.next(3))
        val wrapped = ReelPosition(ReelStage.Shot, 2).next(3)
        assertEquals(ReelPosition(ReelStage.Player), wrapped)
        assertEquals(ReelPosition(ReelStage.Shot, 2), wrapped.previous(wrapBrowse = true, shotCount = 3))
    }
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
    fun regionAnswersUseTheServersScoreAndKeepLegacyNumberedAnswers() {
        assertFalse(ChallengeAnswer("wrong-region",-2,6,100,scoredCorrect=false).correct)
        assertTrue(ChallengeAnswer("right-region",-2,6,100,scoredCorrect=true).correct)
        assertTrue(ChallengeAnswer("legacy",6,6,100).correct)
        val attempt=DailyChallengeAttempt(DailyChallenge("2026-11-10",listOf("a","b")),listOf(
            ChallengeAnswer("a",-2,6,100,scoredCorrect=true),
            ChallengeAnswer("b",-2,6,200,scoredCorrect=false)))
        assertEquals(1,attempt.correctCount)
        assertTrue(attempt.complete)
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

    @Test fun browsingSkipsIntroductionsOnlyWithinRunsAndBacktrackingIsSymmetric() {
        val firstLayout = ReelPosition(ReelStage.Shot, 0)
        val secondLayout = firstLayout.next(4, nextStartsGroup = false)
        assertEquals(ReelPosition(ReelStage.Shot, 1), secondLayout)
        assertEquals(firstLayout, secondLayout.previous(currentStartsGroup = false))
        val nextIntro = secondLayout.next(4, nextStartsGroup = true)
        assertEquals(ReelPosition(ReelStage.Player, 2), nextIntro)
        assertEquals(secondLayout, nextIntro.previous())
        assertEquals(nextIntro, nextIntro.next(4).previous(currentStartsGroup = true))
        // Daily/default navigation still introduces every independent shot.
        assertEquals(ReelPosition(ReelStage.Player, 1), firstLayout.next(4))
        assertEquals(ReelPosition(ReelStage.Player, 0), ReelPosition(ReelStage.Shot, 3).next(4))
    }
}
