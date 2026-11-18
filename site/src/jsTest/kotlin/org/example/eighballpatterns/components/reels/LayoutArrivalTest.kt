package org.example.eighballpatterns.components.reels

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutArrivalTest {
    private fun withImage(check: (HTMLElement) -> Unit) {
        ensureEightBallReelsStyles()
        val image = document.createElement("div") as HTMLElement
        image.className = "layout-image-frame"
        image.style.cssText = "position:absolute;left:-10000px;" +
            "--layout-width:200px;--layout-height:100px;" +
            "transform:translate(-50%,-50%)"
        document.body!!.appendChild(image)
        try { check(image) } finally { image.remove() }
    }

    @Test fun incomingImageStartsAtNormalSizeWithoutAnimating() = withImage { image ->
        assertEquals(200.0, image.getBoundingClientRect().width, .01)
        assertEquals(100.0, image.getBoundingClientRect().height, .01)
        assertEquals(0, image.asDynamic().getAnimations().length as Int)
    }

    @Test fun manualImageSizingDoesNotIntroduceAnAnimation() = withImage { image ->
        image.style.setProperty("--layout-width", "300px")
        image.style.setProperty("--layout-height", "150px")
        assertEquals(300.0, image.getBoundingClientRect().width, .01)
        assertEquals(150.0, image.getBoundingClientRect().height, .01)
        assertEquals(0, image.asDynamic().getAnimations().length as Int)
        image.style.setProperty("--layout-width", "200px")
        image.style.setProperty("--layout-height", "100px")
        assertEquals(200.0, image.getBoundingClientRect().width, .01)
        assertEquals(100.0, image.getBoundingClientRect().height, .01)
        assertEquals(0, image.asDynamic().getAnimations().length as Int)
    }
}
