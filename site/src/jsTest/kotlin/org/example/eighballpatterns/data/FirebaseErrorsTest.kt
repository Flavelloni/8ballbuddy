package org.example.eighballpatterns.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirebaseErrorsTest {
    @Test fun leaderboardFailuresNameTheAffectedLeaderboard() {
        val missing = firebaseErrorMessageForCode("functions/not-found", "NOT_FOUND", "all-time leaderboard")
        assertTrue(missing.contains("all-time leaderboard isn't available yet"))
        assertFalse(missing.contains("daily challenge"))
        val server = firebaseErrorMessageForCode("functions/internal", "INTERNAL", "daily leaderboard")
        assertTrue(server.contains("daily leaderboard couldn't be loaded"))
        assertFalse(server.contains("connect"))
    }

    @Test fun offlineAndTimeoutFailuresRemainDistinguishable() {
        assertTrue(firebaseErrorMessageForCode("functions/unavailable", null).contains("check your connection"))
        assertTrue(firebaseErrorMessageForCode("functions/internal", "INTERNAL", "all-time leaderboard", online = false).contains("check your connection"))
        assertTrue(firebaseErrorMessageForCode("functions/deadline-exceeded", null).contains("took too long"))
    }

    @Test fun validationMessagesArePreservedAndEmptyFallbacksAreReadable() {
        assertEquals("That name is already taken.", firebaseErrorMessageForCode("functions/already-exists", "That name is already taken."))
        assertEquals("Could not load the all-time leaderboard. Please try again.", firebaseErrorMessageForCode(null, "", "all-time leaderboard"))
    }
}
