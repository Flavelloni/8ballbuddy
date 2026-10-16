package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import org.example.eighballpatterns.model.AnswerClock
import org.w3c.dom.events.Event

@Composable
internal fun ShotClock(shotId: String, running: Boolean, answerClock: AnswerClock? = null, onElapsed: (Int) -> Unit = {}) {
    val clock = answerClock ?: remember(shotId) { AnswerClock({ window.performance.now() }) }
    var seconds by remember(clock) { mutableStateOf(clock.elapsedMs / 1000) }
    val reportElapsed by rememberUpdatedState(onElapsed)

    DisposableEffect(clock, running) {
        if (!running) return@DisposableEffect onDispose {}
        var savedSecond = -1

        fun updateClock() {
            seconds = clock.elapsedMs / 1000
            if (seconds != savedSecond) {
                savedSecond = seconds
                reportElapsed(clock.elapsedMs)
            }
        }

        val onVisibilityChange: (Event) -> Unit = {
            if (document.asDynamic().hidden == true) {
                if (!clock.continuous) clock.pause()
                reportElapsed(clock.elapsedMs)
            } else {
                clock.start()
            }
            updateClock()
        }
        if (clock.continuous || document.asDynamic().hidden != true) clock.start()
        val interval = window.setInterval({ updateClock() }, 250)
        val onPageHide: (Event) -> Unit = { if (!clock.continuous) clock.pause(); reportElapsed(clock.elapsedMs) }
        document.addEventListener("visibilitychange", onVisibilityChange)
        window.addEventListener("pagehide", onPageHide)
        onDispose {
            clock.pause()
            reportElapsed(clock.elapsedMs)
            window.clearInterval(interval)
            document.removeEventListener("visibilitychange", onVisibilityChange)
            window.removeEventListener("pagehide", onPageHide)
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
