package org.example.eighballpatterns.browser

import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event

// Capture activation before a button's own handler disables it or changes the reel.
// Delegation covers native and Silk buttons, including keyboard activation and new reels.
internal fun installButtonFeedback(root: Element): () -> Unit {
    val timers = mutableMapOf<HTMLElement, Int>()
    val onClick: (Event) -> Unit = click@{ event ->
        val button = (event.target as? Element)?.closest("button") as? HTMLElement ?: return@click
        if (!root.contains(button) || button.matches(":disabled, [aria-disabled=true]") ||
            button.closest("[inert], .reel-page-preview") != null) return@click

        timers.remove(button)?.let { window.clearTimeout(it) }
        button.classList.add("reel-button-pressed")
        timers[button] = window.setTimeout({
            button.classList.remove("reel-button-pressed")
            timers.remove(button)
        }, 160)

        // Ball selection already has its own haptic pulse. Unsupported browsers keep visual feedback.
        if (!button.classList.contains("layout-ball-button") &&
            !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            try {
                val navigator = window.navigator.asDynamic()
                if (js("typeof navigator.vibrate === 'function'") as Boolean) navigator.vibrate(10)
            } catch (_: Throwable) { }
        }
    }
    root.addEventListener("click", onClick, true)
    return {
        root.removeEventListener("click", onClick, true)
        timers.forEach { (button, timer) ->
            window.clearTimeout(timer)
            button.classList.remove("reel-button-pressed")
        }
        timers.clear()
    }
}
