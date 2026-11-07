package org.example.eighballpatterns.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class YouTubeExperimentTest {
    @Test
    fun sampleUsesOriginalBroadcastSecondsNotLocalSegmentSeconds() {
        assertEquals("AMDgls4nJhQ/052056", youtubeExperimentShot.shotId)
        assertEquals(18960.0 + 340.4, youtubeExperimentShot.start)
        assertEquals(18960.0 + 348.5, youtubeExperimentShot.end)
        assertEquals(8.1, youtubeExperimentShot.end - youtubeExperimentShot.start, 0.0001)
    }

    @Test
    fun adsBufferingAndWaitingDoNotConsumeTheContentWindow() {
        assertFalse(youtubeExperimentShot.hasEnded(15.0))
        assertFalse(youtubeExperimentShot.hasEnded(19307.0))
        assertFalse(youtubeExperimentShot.hasEnded(Double.NaN))
        assertTrue(youtubeExperimentShot.hasEnded(19308.5))
    }

    @Test
    fun invalidIdsAndTimeRangesAreRejected() {
        assertFailsWith<IllegalArgumentException> { YouTubeShotSegment("sample", "bad", 0.0, 8.0) }
        assertFailsWith<IllegalArgumentException> { youtubeExperimentShot.copy(end = youtubeExperimentShot.start) }
        assertFailsWith<IllegalArgumentException> { youtubeExperimentShot.copy(start = -1.0) }
        assertFailsWith<IllegalArgumentException> { youtubeExperimentShot.copy(end = Double.POSITIVE_INFINITY) }
    }
}
