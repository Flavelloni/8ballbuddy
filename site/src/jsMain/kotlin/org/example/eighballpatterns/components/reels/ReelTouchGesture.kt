package org.example.eighballpatterns.components.reels

internal fun isReelViewportZoomed(scale: Double): Boolean = scale > 1.01

/** A pinch, cancellation or zoomed pan must never finish as a reel swipe. */
internal class ReelTouchGesture {
    private var swipeActive = false

    fun start(touchCount: Int, scale: Double): Boolean {
        swipeActive = touchCount == 1 && !isReelViewportZoomed(scale)
        return swipeActive
    }

    fun end(remainingTouches: Int, changedTouches: Int, scale: Double): Boolean {
        val wasSwipe = swipeActive && remainingTouches == 0 && changedTouches == 1 && !isReelViewportZoomed(scale)
        cancel()
        return wasSwipe
    }

    fun cancel() {
        swipeActive = false
    }
}
