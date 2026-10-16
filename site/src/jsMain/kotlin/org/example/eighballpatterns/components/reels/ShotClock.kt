package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.components.icons.fa.FaStopwatch
import com.varabyte.kobweb.silk.components.text.SpanText
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.w3c.dom.events.Event

@Composable
internal fun ShotClock(shotId: String, running: Boolean) {
    var seconds by remember(shotId) { mutableStateOf(0) }

    DisposableEffect(shotId, running) {
        if (!running) return@DisposableEffect onDispose {}

        var accumulatedMs = 0.0
        var visibleSince: Double? = if (document.asDynamic().hidden == true) null else window.performance.now()

        fun updateClock() {
            val currentMs = visibleSince?.let { window.performance.now() - it } ?: 0.0
            seconds = ((accumulatedMs + currentMs) / 1000).toInt()
        }

        val onVisibilityChange: (Event) -> Unit = {
            if (document.asDynamic().hidden == true) {
                visibleSince?.let { accumulatedMs += window.performance.now() - it }
                visibleSince = null
            } else if (visibleSince == null) {
                visibleSince = window.performance.now()
            }
            updateClock()
        }
        val interval = window.setInterval({ updateClock() }, 250)
        document.addEventListener("visibilitychange", onVisibilityChange)
        onDispose {
            window.clearInterval(interval)
            document.removeEventListener("visibilitychange", onVisibilityChange)
        }
    }

    val minutes = (seconds / 60).toString().padStart(2, '0')
    val remainder = (seconds % 60).toString().padStart(2, '0')
    Row(
        Modifier.width(104.px).height(28.px).gap(8.px).color(Color.rgb(0xADE4C5))
            .fontSize(1.cssRem)
            .styleModifier {
                property("justify-content", "center")
                property("font-variant-numeric", "tabular-nums")
            }
            .attrsModifier {
                attr("role", "timer")
                attr("aria-label", "Shot clock")
                attr("aria-live", "off")
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FaStopwatch()
        SpanText("$minutes:$remainder")
    }
}
