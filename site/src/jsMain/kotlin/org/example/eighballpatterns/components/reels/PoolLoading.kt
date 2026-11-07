package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.components.widgets.PoolBall
import org.jetbrains.compose.web.css.px

@Composable
internal fun PoolLoading(label: String) {
    Row(Modifier.gap(8.px).attrsModifier { attr("role", "status"); attr("aria-label", label) },
        verticalAlignment = Alignment.CenterVertically) {
        for (ball in 1..3) {
            Box(Modifier.width(22.px).height(22.px)
                .attrsModifier { classes("pool-loading-ball"); attr("aria-hidden", "true") }
                // Staggered keyframes have no typed Modifier animation equivalent here.
                .styleModifier { property("animation-delay", "${(ball - 1) * 0.2}s") }) {
                PoolBall(ball, fillParent = true)
            }
        }
        SpanText(label)
    }
}
