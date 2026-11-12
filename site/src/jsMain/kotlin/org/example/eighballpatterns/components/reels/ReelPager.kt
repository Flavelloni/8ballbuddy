package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.styleModifier
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import kotlin.math.abs

/** Only three screens are mounted. Neighbours are inert previews, never playing videos
 * or starting daily timers. The browser owns dragging, momentum and scroll snapping. */
internal class ReelPager {
    var element: HTMLElement? = null
    private var pageKey: Any? = null
    private var origin = 0.0
    private var previous = false
    private var next = false
    private var timer: Int? = null
    private var pending: (() -> Unit)? = null
    private var touching = false
    var onSettled: (Int) -> Unit = {}

    fun sync(key: Any, hasPrevious: Boolean, hasNext: Boolean) {
        val el = element ?: return
        val top = if (hasPrevious) el.clientHeight.toDouble() else 0.0
        if (pageKey != key || previous != hasPrevious || origin != top) {
            timer?.let { window.clearTimeout(it) }
            timer = null
            pending = null
            pageKey = key
            previous = hasPrevious
            origin = top
            // Instant recenter after promoting a neighbour; no outgoing-video rewind.
            scrollTo(top, smooth = false)
        }
        next = hasNext
    }

    fun navigate(direction: Int, commit: () -> Unit) {
        if (pending != null) return
        if (element == null || !(if (direction > 0) next else previous)) {
            commit()
            return
        }
        pending = commit
        scrollTo(origin + direction * (element?.clientHeight ?: 0), smooth =
            !window.matchMedia("(prefers-reduced-motion: reduce)").matches)
        scheduleSettle()
    }

    fun touch(active: Boolean) {
        touching = active
        if (!active) scheduleSettle()
    }

    fun scheduleSettle() {
        timer?.let { window.clearTimeout(it) }
        // Fallback for browsers without scrollend; never commit while a finger is held.
        timer = window.setTimeout({ settle() }, 180)
    }

    fun settle() {
        val el = element ?: return
        if (touching || isReelViewportZoomed((window.asDynamic().visualViewport?.scale as? Double) ?: 1.0)) return
        val direction = settledReelDirection(el.scrollTop, origin, el.clientHeight.toDouble())
        if (direction == 0 || !(if (direction > 0) next else previous)) return
        val action = pending
        pending = null
        // The page identity changes synchronously in the owner, then sync recentres it.
        if (action != null) action() else onSettled(direction)
    }

    private fun scrollTo(top: Double, smooth: Boolean) {
        val options = js("({})")
        options.top = top
        options.behavior = if (smooth) "smooth" else "instant"
        element?.asDynamic()?.scrollTo(options)
    }

    fun resize() {
        pageKey?.let { sync(it, previous, next) }
    }

    fun dispose() {
        timer?.let { window.clearTimeout(it) }
        pending = null
        element = null
    }
}

internal fun settledReelDirection(top: Double, origin: Double, height: Double): Int {
    if (height <= 0) return 0
    val offset = (top - origin) / height
    return when {
        abs(offset - 1) < .005 -> 1
        abs(offset + 1) < .005 -> -1
        else -> 0
    }
}

@Composable
internal fun ReelScrollPages(
    pager: ReelPager,
    pageKey: Any,
    previousKey: Any?,
    nextKey: Any?,
    locked: Boolean,
    onSettled: (Int) -> Unit,
    preview: @Composable (Any) -> Unit,
    content: @Composable () -> Unit,
) {
    val settled = rememberUpdatedState(onSettled)
    SideEffect {
        pager.onSettled = { settled.value(it) }
        pager.sync(pageKey, previousKey != null, nextKey != null)
    }
    DisposableEffect(pager) { onDispose { pager.dispose() } }
    Box(Modifier.fillMaxSize().styleModifier {
        // Native CSS scrolling has no equivalent Kobweb snap/event abstraction.
        property("display", "block")
        property("overflow-y", if (locked) "hidden" else "auto")
        property("scroll-snap-type", "y mandatory")
        property("touch-action", "pan-y")
    }.attrsModifier {
        classes("reel-scrollport")
        attr("aria-label", "Reel pages")
        attr("tabindex", "0")
        ref { el ->
            pager.element = el as HTMLElement
            pager.sync(pageKey, previousKey != null, nextKey != null)
            onDispose { pager.element = null }
        }
        addEventListener("scroll") { pager.scheduleSettle() }
        addEventListener("scrollend") { pager.settle() }
        onWheel { event ->
            if (locked || event.ctrlKey || event.metaKey ||
                isReelViewportZoomed((window.asDynamic().visualViewport?.scale as? Double) ?: 1.0)) return@onWheel
            val wheelDelta = (event.nativeEvent.asDynamic().wheelDeltaY as? Double) ?: 0.0
            // Mouse notches need one full-page step. High-resolution trackpad movement
            // remains native, so it can show two pages during a continuous gesture.
            val discrete = event.deltaMode != 0 ||
                (abs(event.deltaY) >= 80 && abs(wheelDelta) >= 120 && abs(wheelDelta) % 120 == 0.0)
            if (discrete && event.deltaX == 0.0) {
                val direction = if(event.deltaY > 0) 1 else -1
                if ((direction > 0 && nextKey != null) || (direction < 0 && previousKey != null)) {
                    event.preventDefault()
                    pager.navigate(direction) { settled.value(direction) }
                }
            }
        }
        onTouchStart { pager.touch(true) }
        onTouchEnd { if (it.touches.length == 0) pager.touch(false) }
        onTouchCancel { pager.touch(false) }
    }) {
        for (entry in listOfNotNull(previousKey, pageKey, nextKey)) key(entry) {
            val active = entry == pageKey
            Box(Modifier.fillMaxSize().attrsModifier {
                classes("reel-page")
                attr("data-reel-active", active.toString())
                if (!active) {
                    classes("reel-page-preview")
                    attr("aria-hidden", "true")
                    attr("inert", "")
                }
            }) {
                if (active) content() else preview(entry)
            }
        }
    }
}
