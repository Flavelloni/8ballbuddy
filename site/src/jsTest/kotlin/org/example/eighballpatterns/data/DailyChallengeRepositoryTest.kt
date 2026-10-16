package org.example.eighballpatterns.data

import kotlinx.browser.window
import org.example.eighballpatterns.model.DailyChallengeAttempt
import org.example.eighballpatterns.model.EightBallEntry
import org.example.eighballpatterns.model.EightBallMeta
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.createDailyChallenge
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DailyChallengeRepositoryTest {
    private val date = "2099-12-31"
    private val storageKey = "eight-ball-daily-v1-$date"
    private val shot = EightBallShot(EightBallEntry("shot", "meta", "image", "video"), EightBallMeta("Player", "Opponent", 1, 6, ""))
    private val shots = listOf(shot)

    @AfterTest
    fun clearSavedRound() {
        window.localStorage.removeItem(storageKey)
    }

    @Test
    fun savesAndRestoresAnswersAndPendingTime() {
        val pending = DailyChallengeAttempt(createDailyChallenge(date, shots), currentShotElapsedMs = 1250)
        assertTrue(saveDailyChallenge(pending))
        assertEquals(pending, loadDailyChallenge(date, shots))
        val complete = pending.answer(shot, 6, 2345)
        assertTrue(saveDailyChallenge(complete))
        assertEquals(complete, loadDailyChallenge(date, shots))
    }

    @Test
    fun invalidJsonStartsAFreshRound() {
        window.localStorage.setItem(storageKey, "{invalid")
        assertEquals(DailyChallengeAttempt(createDailyChallenge(date, shots)), loadDailyChallenge(date, shots))
    }

    @Test
    fun removedShotsInvalidateSavedRound() {
        assertTrue(saveDailyChallenge(DailyChallengeAttempt(createDailyChallenge(date, shots))))
        val replacement = shot.copy(entry = shot.entry.copy(id = "replacement"))
        assertEquals(listOf("replacement"), loadDailyChallenge(date, listOf(replacement)).challenge.shotIds)
    }

    @Test
    fun browserStorageErrorsDoNotCrashSaving() {
        val prototype = window.localStorage.asDynamic().constructor.prototype
        val original = prototype.setItem
        try {
            prototype.setItem = js("(function() { throw new DOMException('Storage blocked', 'SecurityError'); })")
            assertFalse(saveDailyChallenge(DailyChallengeAttempt(createDailyChallenge(date, shots))))
        } finally {
            prototype.setItem = original
        }
    }
}
