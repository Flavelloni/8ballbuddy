package org.example.eighballpatterns.components.widgets

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.left
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.top
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px

private val ballColors = listOf(
    Color.rgb(243, 197, 31),
    Color.rgb(32, 79, 189),
    Color.rgb(209, 40, 40),
    Color.rgb(90, 39, 143),
    Color.rgb(236, 118, 35),
    Color.rgb(35, 123, 75),
    Color.rgb(138, 47, 32),
)

@Composable
fun PoolBall(
    ballNumber: Int,
    modifier: Modifier = Modifier,
    fillParent: Boolean = false,
) {
    require(ballNumber in 1..15)

    val color = if (ballNumber == 8) Color.rgb(17, 17, 17) else ballColors[(ballNumber - 1) % 8]
    val isStripe = ballNumber >= 9
    val size = if (fillParent) 100.percent else 33.px

    Box(
        modifier
            .position(Position.Relative)
            .width(size)
            .height(size)
            .borderRadius(50.percent)
            .overflow(Overflow.Hidden)
            .backgroundColor(if (isStripe) Colors.White else color)
            .border(1.px, LineStyle.Solid, Color.rgba(8, 10, 14, 0.46f))
            .styleModifier {
                property("box-sizing", "border-box")
                property("box-shadow", "0 10px 20px rgba(0,0,0,.32), inset 0 9px 13px rgba(255,255,255,.30), inset 0 -12px 17px rgba(0,0,0,.30)")
                property("background-image", "radial-gradient(circle at 30% 24%, rgba(255,255,255,.92), rgba(255,255,255,.16) 23%, rgba(0,0,0,.18) 78%)")
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isStripe) {
            Box(
                Modifier
                    .position(Position.Absolute)
                    .left(0.percent)
                    .top(31.percent)
                    .width(100.percent)
                    .height(38.percent)
                    .backgroundColor(color)
                    .styleModifier {
                        property("box-shadow", "inset 0 4px 7px rgba(255,255,255,.22), inset 0 -5px 9px rgba(0,0,0,.24)")
                    }
            )
        }
        Box(
            Modifier
                .width(64.percent)
                .height(64.percent)
                .borderRadius(50.percent)
                .backgroundColor(Colors.White)
                .color(Colors.Black)
                .fontSize(12.px)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1)
                .zIndex(2)
                .styleModifier { property("box-shadow", "inset 0 -1px 2px rgba(0,0,0,.22)") },
            contentAlignment = Alignment.Center,
        ) {
            SpanText(ballNumber.toString())
        }
    }
}
