package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowseGroupsTest {
    private fun shot(time: String, player: String = "Erik", breakShot: Boolean = false,
                     match: String = "match", video: String = "8RgRvKQFZvI", archived: Boolean = false,
                     ballGroup: Int = 1): EightBallShot =
        EightBallShot(EightBallEntry("$video/$time", "/8ball/OBK/$video/$match/$time/meta.json?v=abc", "layout.jpg", "clip.mp4"),
            EightBallMeta(player, "", ballGroup, 1, "", isBreak = breakShot, archived = archived))

    @Test fun assignedBallGroupChangesSplitRunsButOpenTableAssignmentDoesNot() {
        for (before in 0..2) for (after in 0..2) {
            val first = shot("020000", ballGroup = before)
            val second = shot("020010", ballGroup = after)
            val split = before in 1..2 && after != before
            assertEquals(if (split) 2 else 1, browseShotGroups(listOf(second, first)).size,
                "Ball group $before -> $after")
        }
        val run = listOf(shot("020000", ballGroup = 0), shot("020010", ballGroup = 1),
            shot("020020", ballGroup = 1), shot("020030", ballGroup = 2))
        assertEquals(listOf(run.take(3), run.takeLast(1)), browseShotGroups(run))
    }

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
        assertEquals(5, ordered.size)
        assertTrue(ordered.take(4).all { it.meta.player == "Erik" })
        assertEquals("Ronny", ordered.last().meta.player)
        for (group in groups) {
            val index = ordered.indexOf(group.first())
            assertEquals(group, ordered.subList(index, index + group.size))
        }
        assertEquals(ordered, shuffledBrowseShots(shots, "Erik", 42))
        assertTrue((1..20).map { shuffledBrowseShots(shots, null, it) }.distinct().size > 1)
    }

    @Test fun availablePlayersComeOnlyFromActiveShotShooters() {
        val shots = listOf(shot("020000", "Erik"), shot("020010", " erik "), shot("020020", "Ronny"), shot("020030", "Retired", archived = true))
        assertEquals(listOf("Erik", "Ronny"), availableBrowsePlayers(shots))
        assertEquals(shuffledBrowseShots(shots, null, 1), shuffledBrowseShots(shots, "Not available", 1))
    }

    @Test fun preferredPlayerThenOtherPlayersStayGroupedRandomAndDoNotRepeat() {
        val shots = listOf(shot("020000"), shot("020010"), shot("020020", "Ronny"),
            shot("020030", "Ronny"), shot("020040"), shot("020050", "Lance"), shot("020100", "Lance"))
        val orders = (1..20).map { seed ->
            shuffledBrowseShots(shots, "Erik", seed).also { ordered ->
                assertEquals(shots.size, ordered.size)
                assertEquals(shots.toSet(), ordered.toSet())
                assertTrue(ordered.take(3).all { it.meta.player == "Erik" })
                assertTrue(ordered.drop(3).none { it.meta.player == "Erik" })
                for (group in browseShotGroups(shots)) {
                    val index = ordered.indexOf(group.first())
                    assertEquals(group, ordered.subList(index, index + group.size))
                }
            }
        }
        assertTrue(orders.map { it.take(3) }.distinct().size > 1)
        assertTrue(orders.map { it.drop(3) }.distinct().size > 1)
    }
}
