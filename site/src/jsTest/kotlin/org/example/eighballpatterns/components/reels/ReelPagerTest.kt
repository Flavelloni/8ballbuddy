package org.example.eighballpatterns.components.reels

import kotlin.test.Test
import kotlin.test.assertEquals

class ReelPagerTest {
    @Test fun onlyASettledNeighbourCommitsNavigation() {
        assertEquals(0, settledReelDirection(840.0, 840.0, 840.0))
        assertEquals(0, settledReelDirection(1260.0, 840.0, 840.0))
        assertEquals(1, settledReelDirection(1680.0, 840.0, 840.0))
        assertEquals(-1, settledReelDirection(0.0, 840.0, 840.0))
        assertEquals(1, settledReelDirection(1678.0, 840.0, 840.0))
        assertEquals(0, settledReelDirection(0.0, 0.0, 0.0))
    }
}
