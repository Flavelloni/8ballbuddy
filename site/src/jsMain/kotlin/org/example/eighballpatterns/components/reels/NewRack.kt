package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.components.widgets.PoolBall
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px

@Composable
internal fun EightBallNewRack() {
    Box(Modifier.fillMaxSize().padding(32.px).attrsModifier { attr("aria-label", "New rack") },
        contentAlignment = Alignment.Center) {
        Column(Modifier.gap(20.px), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.gap(8.px)) { for (ball in listOf(1, 8, 12)) PoolBall(ball) }
            SpanText("New rack", Modifier.fontSize(2.4.cssRem).fontWeight(FontWeight.Bold))
            SpanText("A fresh table. Take another look.", Modifier.fontSize(0.95.cssRem).color(Color.rgb(0x94C9B5)))
            SpanText("Swipe to meet the player", Modifier.fontSize(0.8.cssRem).color(Color.rgb(0xA9B5AF)))
        }
    }
}
