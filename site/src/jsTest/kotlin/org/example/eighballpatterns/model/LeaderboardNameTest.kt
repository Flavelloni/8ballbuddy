package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LeaderboardNameTest {
    @Test fun renameUpdatesOwnRowWithoutChangingRanksOrScores() {
        val other = LeaderboardEntry("other", "Other player", 20, 24, 100, 1)
        val own = LeaderboardEntry("me", "Old name", 3, 6, 200, 7)
        val original = DailyLeaderboard(listOf(other, own), total = 8, ownRank = 7, ownEntry = own)
        val renamed = original.withPlayerName("me", "New name")
        assertEquals(other, renamed.entries.first())
        assertEquals(own.copy(displayName = "New name"), renamed.entries.last())
        assertEquals(own.copy(displayName = "New name"), renamed.ownEntry)
        assertEquals(7, renamed.ownRank)
        assertEquals(8, renamed.total)
        assertEquals("Old name", original.ownEntry?.displayName)
    }
}
