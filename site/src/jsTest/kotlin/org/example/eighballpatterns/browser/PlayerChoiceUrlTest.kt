package org.example.eighballpatterns.browser

import com.varabyte.kobweb.navigation.BasePath
import kotlinx.browser.window
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PlayerChoiceUrlTest {
    @Test
    fun chooserHasCleanBasePathAwareUrlAndSelectionReturnsToShotUrls() {
        val originalUrl = window.location.href
        try {
            updateEightBallChallengeUrl("2026-11-18")
            updateEightBallPlayerChoiceUrl()
            assertEquals(BasePath.prependTo("/players"), window.location.pathname)
            assertEquals("", window.location.search)
            assertEquals("", window.location.hash)

            updateEightBallReelsUrl("QymvyRiXJ9A/030318", playerIntro = true)
            assertEquals(BasePath.prependTo("/"), window.location.pathname)
            assertEquals("QymvyRiXJ9A/030318", currentEightBallReelsShotId())
            assertEquals(true, currentEightBallReelsPlayerIntro())
            assertFalse(currentEightBallDailyChallenge())

            updateEightBallPlayerChoiceUrl()
            updateEightBallReelsUrl(null, playerIntro = false)
            assertEquals(BasePath.prependTo("/"), window.location.pathname)
            assertEquals("", window.location.search)
        } finally {
            window.history.replaceState(null, "", originalUrl)
        }
    }
}
