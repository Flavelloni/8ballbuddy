package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BreakPredictionTest {
    private val entry = EightBallEntry("break", "meta.json", "layout.jpg", "clip.mp4")
    private val meta = EightBallMeta("Lance Ireland", "Rob Hall", 0, -1, "", isBreak = true, ballsPotted = 0)

    @Test
    fun breakOffersCountsZeroThroughFourInsteadOfNumberedBalls() {
        assertEquals(listOf(0, 1, 2, 3, 4), eightBallReelPickerBalls(EightBallShot(entry, meta)))
        assertEquals("Lance Ireland is breaking", eightBallReelTitle(meta))
        assertEquals((1..7).toList() + 0, eightBallReelPickerBalls(EightBallShot(entry, meta.copy(isBreak = false, group = 1))))
        assertEquals((9..15).toList() + 0, eightBallReelPickerBalls(EightBallShot(entry, meta.copy(isBreak = false, group = 2))))
    }

    @Test
    fun playerStatusDoesNotRepeatTheSeparatelyDisplayedName() {
        assertEquals("Is breaking", eightBallPlayerStatus(meta))
        assertEquals("Open table", eightBallPlayerStatus(meta.copy(isBreak = false)))
        assertEquals("Shoots solids", eightBallPlayerStatus(meta.copy(isBreak = false, group = 1)))
        assertEquals("Shoots stripes", eightBallPlayerStatus(meta.copy(isBreak = false, group = 2)))
        assertEquals("Shoots the 8-ball", eightBallPlayerStatus(meta.copy(isBreak = false, group = 3)))
    }

    @Test
    fun breakRevealReportsCountIncludingZeroAndSingularOne() {
        assertEquals("Correct: 0 balls potted on the break", eightBallReelResultText(0, meta))
        assertEquals("You guessed 2. Correct: 0 balls potted on the break", eightBallReelResultText(2, meta))
        assertEquals("Correct: 1 ball potted on the break", eightBallReelResultText(1, meta.copy(ballsPotted = 1)))
        assertEquals("Correct: 4 balls potted on the break", eightBallReelResultText(4, meta.copy(ballsPotted = 4)))
        assertEquals("Correct: 5 ball", eightBallReelResultText(5, meta.copy(isBreak = false, ball = 5)))
        assertFalse(buildEightBallReelFeedback(false, isBreak = true).contains("ball"))
        assertTrue(EightBallReelStats().next(0 == meta.correctAnswer).correct == 1)
    }
}
