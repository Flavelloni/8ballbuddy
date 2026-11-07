package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowseGroupsTest {
    private fun shot(time: String, player: String = "Erik", breakShot: Boolean = false,
                     match: String = "match", video: String = "8RgRvKQFZvI", archived: Boolean = false): EightBallShot =
        EightBallShot(EightBallEntry("$video/$time", "/8ball/OBK/$video/$match/$time/meta.json?v=abc", "layout.jpg", "clip.mp4"),
            EightBallMeta(player, "", 1, 1, "", isBreak = breakShot, archived = archived))

    @Test fun allFiveBoundariesCreateGroupsAndExactlyThreeMinutesDoesNot() {
        val a = shot("020000")
        val b = shot("020300")
        val c = shot("020601")
        val d = shot("020610", breakShot = true)
        val e = shot("020620", player = "Ronny")
        val f = shot("020630", player = "Ronny", match = "other-match")
        val g = shot("020640", player = "Ronny", video = "QymvyRiXJ9A")
        val groups = browseShotGroups(listOf(g, f, e, d, c, b, a))
        assertEquals(6, groups.size)
        assertTrue(groups.contains(listOf(a, b)))
        for (single in listOf(c, d, e, f, g)) assertTrue(groups.contains(listOf(single)))
    }

    @Test fun filteringDoesNotMergeSeparateTurnsAndShuffleNeverSplitsRuns() {
        val shots = listOf(shot("020000"), shot("020010"), shot("020020", "Ronny"), shot("020030"), shot("020040"))
        val groups = browseShotGroups(shots)
        assertEquals(3, groups.size)
        val ordered = shuffledBrowseShots(shots, " erik ", 42)
        assertEquals(4, ordered.size)
        assertTrue(ordered.all { it.meta.player == "Erik" })
        for (group in groups.filter { it.first().meta.player == "Erik" }) {
            val index = ordered.indexOf(group.first())
            assertEquals(group, ordered.subList(index, index + group.size))
        }
        assertEquals(ordered, shuffledBrowseShots(shots, "Erik", 42))
        assertTrue((1..20).map { shuffledBrowseShots(shots, null, it) }.distinct().size > 1)
    }

    @Test fun availablePlayersComeOnlyFromActiveShotShooters() {
        val shots = listOf(shot("020000", "Erik"), shot("020010", " erik "), shot("020020", "Ronny"), shot("020030", "Retired", archived = true))
        assertEquals(listOf("Erik", "Ronny"), availableBrowsePlayers(shots))
        assertTrue(shuffledBrowseShots(shots, "Not available", 1).isEmpty())
    }
}
