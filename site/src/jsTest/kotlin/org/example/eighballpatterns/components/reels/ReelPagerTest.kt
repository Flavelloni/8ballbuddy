package org.example.eighballpatterns.components.reels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.js.Promise
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.events.WheelEventInit

class ReelPagerTest {
    private fun frames(): Promise<Unit> = Promise { resolve, _ ->
        window.requestAnimationFrame { window.requestAnimationFrame { window.requestAnimationFrame { resolve(Unit) } } }
    }

    private fun withPager(check: (ReelPager, HTMLElement) -> Unit): Promise<Unit> {
        val port = document.createElement("div") as HTMLElement
        port.style.cssText = "position:absolute;left:-10000px;width:100px;height:100px;overflow-y:auto"
        repeat(3) {
            val page = document.createElement("div") as HTMLElement
            page.style.height = "100px"
            port.appendChild(page)
        }
        document.body!!.appendChild(port)
        val pager = ReelPager()
        pager.element = port
        pager.sync("first", true, true)
        return frames().then {
            try { check(pager, port) } finally { pager.dispose(); port.remove() }
        }
    }

    @Test fun pixelWheelBurstsUseOnePageAnimationWithoutNativeScrolling(): Promise<Unit> = withPager { pager, port ->
        val directions = mutableListOf<Int>()
        pager.onSettled = { directions.add(it) }
        // Synthetic WheelEvents have no legacy mouse-notch value. All packets must still
        // be cancelled, including the small trailing packets from smooth/free-spin wheels.
        for (delta in listOf(120.0, 45.0, 12.0, 3.0)) {
            val event = WheelEvent("wheel", WheelEventInit(deltaY = delta, cancelable = true))
            pager.wheel(event)
            assertTrue(event.defaultPrevented)
        }
        port.scrollTop = 200.0
        pager.settle()
        assertEquals(listOf(1), directions)
    }

    @Test fun backwardWheelOverridesAutomaticForwardScrolling(): Promise<Unit> = withPager { pager, port ->
        val directions = mutableListOf<Int>()
        pager.onSettled = { directions.add(it) }
        assertTrue(pager.navigate(1, automatically = true))
        val event = WheelEvent("wheel", WheelEventInit(deltaY = -45.0, cancelable = true))
        pager.wheel(event)
        assertTrue(event.defaultPrevented)
        port.scrollTop = 0.0
        pager.settle()
        assertEquals(listOf(-1), directions)
    }

    @Test fun uncancelableNativeWheelCannotStartACompetingAnimation(): Promise<Unit> = withPager { pager, port ->
        var scrollCalls = 0
        port.asDynamic().scrollTo = { _: dynamic -> scrollCalls++ }
        pager.wheel(WheelEvent("wheel", WheelEventInit(deltaY = 45.0, cancelable = false)))
        assertEquals(0, scrollCalls)
    }

    @Test fun zoomAndHorizontalWheelGesturesAreNotConsumed(): Promise<Unit> = withPager { pager, _ ->
        val events = listOf(
            WheelEvent("wheel", WheelEventInit(deltaY = 45.0, ctrlKey = true, cancelable = true)),
            WheelEvent("wheel", WheelEventInit(deltaY = 45.0, metaKey = true, cancelable = true)),
            WheelEvent("wheel", WheelEventInit(deltaX = 60.0, deltaY = 5.0, cancelable = true)),
            WheelEvent("wheel", WheelEventInit(deltaY = 0.0, cancelable = true)),
        )
        for (event in events) {
            pager.wheel(event)
            assertFalse(event.defaultPrevented)
        }
    }

    @Test fun wheelPacketsDuringRecenteringCannotNativelyMoveRecycledPages(): Promise<Unit> = withPager { pager, _ ->
        pager.sync("second", true, true)
        val event = WheelEvent("wheel", WheelEventInit(deltaY = 45.0, cancelable = true))
        pager.wheel(event)
        assertTrue(event.defaultPrevented)
        assertFalse(pager.navigate(1))
    }

    @Test fun manualScrollWinsWhenVideoEndsDuringABackwardTransition(): Promise<Unit> = withPager { pager, port ->
        val directions = mutableListOf<Int>()
        pager.onSettled = { directions.add(it) }
        assertTrue(pager.navigate(-1))
        assertFalse(pager.navigate(1, automatically = true))
        port.scrollTop = 0.0
        pager.settle()
        assertEquals(listOf(-1), directions)
    }

    @Test fun backwardInputOverridesAnAutomaticForwardTransition(): Promise<Unit> = withPager { pager, port ->
        val directions = mutableListOf<Int>()
        pager.onSettled = { directions.add(it) }
        assertTrue(pager.navigate(1, automatically = true))
        assertTrue(pager.navigate(-1))
        port.scrollTop = 0.0
        pager.settle()
        assertEquals(listOf(-1), directions)
    }

    @Test fun nativeSwipeAndRecentManualInputPreventAutomaticAdvancement(): Promise<Unit> = withPager { pager, port ->
        pager.touch(true)
        assertFalse(pager.navigate(1, automatically = true))
        pager.touch(false)
        assertFalse(pager.navigate(1, automatically = true))
        port.scrollTop = 140.0
        pager.sync("first", true, true)
        assertEquals(140.0, port.scrollTop)
    }

    @Test fun duplicateScrollEndAndRecenteringCannotCommitAnotherPage(): Promise<Unit> = withPager { pager, port ->
        val directions = mutableListOf<Int>()
        pager.onSettled = {
            directions.add(it)
            pager.sync("second", true, true)
        }
        port.scrollTop = 200.0
        pager.settle()
        pager.settle()
        assertEquals(listOf(1), directions)
        assertEquals(100.0, port.scrollTop)
    }

    @Test fun onlyASettledNeighbourCommitsNavigation() {
        assertEquals(0, settledReelDirection(840.0, 840.0, 840.0))
        assertEquals(0, settledReelDirection(1260.0, 840.0, 840.0))
        assertEquals(1, settledReelDirection(1680.0, 840.0, 840.0))
        assertEquals(-1, settledReelDirection(0.0, 840.0, 840.0))
        assertEquals(1, settledReelDirection(1678.0, 840.0, 840.0))
        assertEquals(0, settledReelDirection(0.0, 0.0, 0.0))
    }
}
