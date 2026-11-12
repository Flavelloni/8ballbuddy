package org.example.eighballpatterns.components.reels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.js.Promise
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

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
