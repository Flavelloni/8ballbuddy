package org.example.eighballpatterns.components.reels

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReelTouchGestureTest {
    @Test
    fun ordinaryOneFingerSwipeStillNavigates() {
        val gesture = ReelTouchGesture()
        assertTrue(gesture.start(1, 1.0))
        assertTrue(gesture.end(0, 1, 1.0))
        assertFalse(gesture.end(0, 1, 1.0))
    }

    @Test
    fun addingSecondFingerCancelsSwipeUntilBothFingersAreReleased() {
        val gesture = ReelTouchGesture()
        assertTrue(gesture.start(1, 1.0))
        assertFalse(gesture.start(2, 1.0))
        assertFalse(gesture.end(1, 1, 1.0))
        assertFalse(gesture.end(0, 1, 1.0))
        assertTrue(gesture.start(1, 1.0))
        assertTrue(gesture.end(0, 1, 1.0))
    }

    @Test
    fun zoomedPanningAndBrowserCancellationNeverNavigate() {
        val gesture = ReelTouchGesture()
        assertFalse(gesture.start(1, 2.0))
        assertFalse(gesture.end(0, 1, 2.0))
        assertTrue(gesture.start(1, 1.0))
        assertFalse(gesture.end(0, 1, 1.5))
        assertTrue(gesture.start(1, 1.0))
        gesture.cancel()
        assertFalse(gesture.end(0, 1, 1.0))
    }

    @Test
    fun SimultaneousFingerReleaseDoesNotCountAsSwipe() {
        val gesture = ReelTouchGesture()
        gesture.start(1, 1.0)
        assertFalse(gesture.end(0, 2, 1.0))
    }
}
