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
import kotlinx.browser.window
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.example.eighballpatterns.model.AnswerClock

@Composable
internal fun ShotClock(running: Boolean, answerClock: AnswerClock) {
    val clock = answerClock
    var seconds by remember(clock) { mutableStateOf(clock.elapsedMs / 1000) }
    DisposableEffect(clock, running) {
        if (running) clock.start()
        seconds = clock.elapsedMs / 1000
        val interval = if (running) window.setInterval({ seconds = clock.elapsedMs / 1000 }, 250) else null
        onDispose {
            clock.pause()
            if (interval != null) window.clearInterval(interval)
        }
    }

    val minutes = (seconds / 60).toString().padStart(2, '0')
    val remainder = (seconds % 60).toString().padStart(2, '0')
    Row(
        Modifier.width(144.px).height(44.px).gap(10.px).color(Color.rgb(0xD5F5E4))
            .fontSize(1.4.cssRem)
            .styleModifier {
                property("justify-content", "center")
                property("font-variant-numeric", "tabular-nums")
                property("font-weight", "750")
                property("background", "#10271f")
                property("border", "1px solid #648d7b")
                property("border-radius", "8px")
                property("box-sizing", "border-box")
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
