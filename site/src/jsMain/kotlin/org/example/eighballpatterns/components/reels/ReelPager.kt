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
    private var restoreFrame: Int? = null
    private var targetDirection = 0
    private var automatic = false
    private var queuedAutomatic: Pair<Any?, Int>? = null
    private var committing = false
    private var touching = false
    private var lastManualInput = Double.NEGATIVE_INFINITY
    private var revision = 0
    var onSettled: (Int) -> Unit = {}

    fun sync(key: Any, hasPrevious: Boolean, hasNext: Boolean) {
        val el = element ?: return
        val top = if (hasPrevious) el.clientHeight.toDouble() else 0.0
        if (pageKey != key || previous != hasPrevious || origin != top) {
            revision++
            if (pageKey != key) queuedAutomatic = null
            timer?.let { window.clearTimeout(it) }
            timer = null
            targetDirection = 0
            automatic = false
            pageKey = key
            previous = hasPrevious
            origin = top
            rebase(top)
        }
        next = hasNext
    }

    fun navigate(direction: Int, automatically: Boolean = false): Boolean {
        val el = element ?: return false
        if (!automatically) manualInput()
        if (committing) {
            if (automatically) queuedAutomatic = pageKey to direction
            return automatically
        }
        if (!(if (direction > 0) next else previous)) return false
        if (automatically && (touching || targetDirection != 0 ||
            abs(el.scrollTop - origin) > 1 || window.performance.now() - lastManualInput < 300)) return false
        if (targetDirection == direction) return true
        targetDirection = direction
        automatic = automatically
        scrollTo(origin + direction * (element?.clientHeight ?: 0), smooth =
            !window.matchMedia("(prefers-reduced-motion: reduce)").matches)
        scheduleSettle()
        return true
    }

    /** User input supersedes automatic advancement, including a reversal mid-animation. */
    fun manualInput() {
        lastManualInput = window.performance.now()
        queuedAutomatic = null
        if (automatic) {
            automatic = false
            targetDirection = 0
            scrollTo(element?.scrollTop ?: origin, smooth = false)
        }
    }

    fun touch(active: Boolean) {
        touching = active
        if (active) manualInput()
        if (!active) scheduleSettle()
    }

    fun scheduleSettle() {
        if (committing) return
        timer?.let { window.clearTimeout(it) }
        // Fallback for browsers without scrollend; never commit while a finger is held.
        timer = window.setTimeout({ settle() }, 180)
    }

    fun settle() {
        val el = element ?: return
        if (committing || touching || isReelViewportZoomed((window.asDynamic().visualViewport?.scale as? Double) ?: 1.0)) return
        val direction = settledReelDirection(el.scrollTop, origin, el.clientHeight.toDouble())
        if (direction == 0) {
            if (abs(el.scrollTop - origin) < 1) { targetDirection = 0; automatic = false }
            return
        }
        if (!(if (direction > 0) next else previous)) { rebase(origin); return }
        timer?.let { window.clearTimeout(it) }
        timer = null
        targetDirection = 0
        automatic = false
        committing = true
        // Stop snapping before Compose changes the neighbours. Otherwise the browser can snap
        // to a recycled page and its scrollend event can commit an unwanted second navigation.
        el.style.setProperty("scroll-snap-type", "none")
        val oldKey = pageKey
        val oldRevision = revision
        onSettled(direction)
        window.setTimeout({
            // A route may reject navigation (e.g. a challenge became busy). Never leave it locked.
            if (element === el && committing && pageKey == oldKey && revision == oldRevision) rebase(origin)
        }, 100)
    }

    private fun rebase(top: Double) {
        val el = element ?: return
        committing = true
        restoreFrame?.let { window.cancelAnimationFrame(it) }
        el.style.setProperty("scroll-snap-type", "none")
        scrollTo(top, smooth = false)
        restoreFrame = window.requestAnimationFrame {
            // Flush the new page positions before restoring snap. Ignore recentering events.
            el.getBoundingClientRect()
            el.style.removeProperty("scroll-snap-type")
            restoreFrame = window.requestAnimationFrame {
                committing = false
                restoreFrame = null
                val queued = queuedAutomatic
                queuedAutomatic = null
                if (queued != null && queued.first == pageKey) navigate(queued.second, automatically = true)
                scheduleSettle()
            }
        }
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
        restoreFrame?.let { window.cancelAnimationFrame(it) }
        targetDirection = 0
        queuedAutomatic = null
        committing = false
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
            pager.manualInput()
            val wheelDelta = (event.nativeEvent.asDynamic().wheelDeltaY as? Double) ?: 0.0
            // A mouse notch can produce a small pixel delta depending on OS scroll settings.
            // Use its notch value, not an arbitrary 80px cutoff, to avoid partial snap-back.
            // High-resolution trackpad movement remains native.
            val discrete = event.deltaMode != 0 ||
                (abs(wheelDelta) >= 120 && abs(wheelDelta) % 120 == 0.0)
            if (discrete && event.deltaX == 0.0) {
                val direction = if(event.deltaY > 0) 1 else -1
                if ((direction > 0 && nextKey != null) || (direction < 0 && previousKey != null)) {
                    event.preventDefault()
                    pager.navigate(direction)
                }
            }
        }
        onTouchStart { pager.touch(true) }
        onTouchEnd { if (it.touches.length == 0) pager.touch(false) }
        onTouchCancel { pager.touch(false) }
    }) {
        // Physical slots stay in place. Moving keyed page elements makes native scroll snap
        // follow their old identity when they change from a neighbour into the active page.
        for ((slot, entry) in listOf(previousKey, pageKey, nextKey).withIndex()) if (entry != null) key(slot) {
            val active = slot == 1
            Box(Modifier.fillMaxSize().attrsModifier {
                classes("reel-page")
                attr("data-reel-active", active.toString())
                if (!active) {
                    classes("reel-page-preview")
                    attr("aria-hidden", "true")
                    attr("inert", "")
                }
            }) {
                key(entry) { if (active) content() else preview(entry) }
            }
        }
    }
}
