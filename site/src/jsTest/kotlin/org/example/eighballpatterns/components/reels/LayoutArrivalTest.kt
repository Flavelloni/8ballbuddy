package org.example.eighballpatterns.components.reels

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LayoutArrivalTest {
    private fun withImage(check: (HTMLElement) -> Unit) {
        ensureEightBallReelsStyles()
        val image = document.createElement("div") as HTMLElement
        image.className = "layout-image-frame layout-arrival-start"
        image.style.cssText = "position:absolute;left:-10000px;" +
            "--layout-width:200px;--layout-height:100px;" +
            "--layout-arrival-width:300px;--layout-arrival-height:150px;" +
            "--layout-pulse-scale:1.5;transform:translate(-50%,-50%)"
        document.body!!.appendChild(image)
        try { check(image) } finally { image.remove() }
    }

    @Test fun incomingPreviewAlreadyHasTheZoomedSizeWithoutAnimating() = withImage { image ->
        assertEquals(300.0, image.getBoundingClientRect().width, .01)
        assertEquals(150.0, image.getBoundingClientRect().height, .01)
        assertEquals(0, image.asDynamic().getAnimations().length as Int)
    }

    @Test fun zoomOutHoldsItsStartingFrameEvenBeforeTheAnimationTimelineBegins() = withImage { image ->
        image.className = "layout-image-frame layout-arrival-pulse"
        val animation = image.asDynamic().getAnimations()[0]
        animation.pause()
        assertEquals(2000.0, (animation.effect.getTiming().duration as Number).toDouble())
        // Chrome can initially report a slightly negative currentTime during a native swipe.
        // Without backwards fill this paints the fitted image, then jumps to zoomed.
        for (time in listOf(-.01, 0.0)) {
            animation.currentTime = time
            assertEquals(300.0, image.getBoundingClientRect().width, .01)
        }
        animation.currentTime = 1000
        assertTrue(image.getBoundingClientRect().width in 200.01..299.99)
        animation.currentTime = 2000
        assertEquals(200.0, image.getBoundingClientRect().width, .01)
    }
}
