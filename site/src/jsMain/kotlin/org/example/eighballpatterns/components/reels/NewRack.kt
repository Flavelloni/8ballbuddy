package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.height
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
    Box(Modifier.fillMaxSize().padding(top=90.px,bottom=32.px,leftRight=32.px).attrsModifier { classes("reel-rack");attr("aria-label", "New rack") },
        contentAlignment = Alignment.Center) {
        Column(Modifier.gap(20.px), horizontalAlignment = Alignment.CenterHorizontally) {
            SpanText("A FRESH PERSPECTIVE",Modifier.attrsModifier {classes("reel-eyebrow")})
            Column(Modifier.gap(4.px),horizontalAlignment=Alignment.CenterHorizontally) {
                for(row in listOf(listOf(1),listOf(4,8),listOf(12,3,10))) Row(Modifier.gap(4.px)) {
                    row.forEach {ball -> Box(Modifier.width(30.px).height(30.px)) {PoolBall(ball,fillParent=true)} }
                }
            }
            H2 {Text("New rack")}
            SpanText("New positions. New possibilities.", Modifier.fontSize(.88.cssRem).color(Color.rgb(0xA1BDAA)))
        }
    }
}
