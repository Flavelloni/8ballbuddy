package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.components.icons.fa.FaAnglesUp
import kotlinx.browser.document
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private const val EIGHT_BALL_WHEEL_THRESHOLD = 56.0

internal fun ensureEightBallReelsStyles() {
    if (document.getElementById("eight-ball-reels-styles") != null) return

    val style = document.createElement("style")
    style.setAttribute("id", "eight-ball-reels-styles")
    style.textContent = """
        @keyframes eightBallReelPulse {
          0%, 100% { opacity: .42; transform: translateY(7px); }
          50% { opacity: 1; transform: translateY(-7px); }
        }
    """.trimIndent()
    document.head?.appendChild(style)
}

@Composable
internal fun EightBallReelIntro(onStart: () -> Unit) {
    Column(
        Modifier
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("justify-content", "center")
                property("gap", "26px")
                property("padding", "34px")
                property("box-sizing", "border-box")
                property("color", "rgba(246,250,246,.9)")
                property("text-align", "center")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        H1(
            attrs = Modifier
                .margin(0.px)
                .fontSize(2.05.cssRem)
                .lineHeight(1.05)
                .fontWeight(FontWeight.Bold)
                .color(Colors.White)
                .toAttrs()
        ) {
            Text("8-Ball Buddy")
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .styleModifier {
                    property("max-width", "320px")
                    property("font-size", "1.16rem")
                    property("font-weight", "750")
                    property("line-height", "1.32")
                }
                .toAttrs()
        ) {
            Text("Can you predict the pro's next move?")
        }
        P(
            attrs = Modifier
                .margin(0.px)
                .styleModifier {
                    property("max-width", "320px")
                    property("font-size", ".92rem")
                    property("font-weight", "600")
                    property("line-height", "1.42")
                    property("color", "rgba(246,250,246,.68)")
                }
                .toAttrs()
        ) {
            Text("Guess which ball the pro pots next, then watch the shot reveal.")
        }

        Button(
            attrs = Modifier
                .borderRadius(999.px)
                .border(1.px, LineStyle.Solid, Color.rgba(126, 218, 116, 0.34f))
                .backgroundColor(Color.rgba(126, 218, 116, 0.14f))
                .color(Colors.White)
                .padding(leftRight = 1.15.cssRem, topBottom = 0.72.cssRem)
                .fontWeight(FontWeight.Bold)
                .styleModifier { property("cursor", "pointer") }
                .toAttrs { onClick { onStart() } }
        ) {
            Text("Start")
        }
        EightBallReelSwipeHint("Swipe to start")
    }
}

@Composable
internal fun EightBallReelsShell(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTouchStart: (Double) -> Unit,
    onTouchEnd: (Double) -> Unit,
    onTouchCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .position(Position.Relative)
            .width(100.percent)
            .styleModifier {
                property("height", "100dvh")
                property("min-height", "100dvh")
                property("background", "radial-gradient(circle at 50% 18%, rgba(32,64,49,.32), transparent 36%), #050606")
                property("overflow", "hidden")
                property("touch-action", "none")
            }
            .attrsModifier {
                onWheel { event ->
                    if (event.deltaY >= EIGHT_BALL_WHEEL_THRESHOLD) {
                        event.preventDefault()
                        onNext()
                    } else if (event.deltaY <= -EIGHT_BALL_WHEEL_THRESHOLD) {
                        event.preventDefault()
                        onPrevious()
                    }
                }
                onTouchStart { event ->
                    val touch = event.touches.item(0) ?: return@onTouchStart
                    onTouchStart(touch.clientY.toDouble())
                }
                onTouchEnd { event ->
                    val touch = event.changedTouches.item(0) ?: return@onTouchEnd
                    onTouchEnd(touch.clientY.toDouble())
                }
                onTouchCancel {
                    onTouchCancel()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .position(Position.Relative)
                .height(100.percent)
                .styleModifier {
                    property("width", "min(100vw, 460px)")
                    property("min-height", "100dvh")
                    property("background", "#020303")
                    property("overflow", "hidden")
                    property("box-shadow", "0 0 80px rgba(0,0,0,.58)")
                }
        ) {
            content()
        }
    }
}

@Composable
internal fun EightBallReelMessage(message: String) {
    Column(
        Modifier
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("justify-content", "center")
                property("color", "rgba(246,250,246,.72)")
                property("font-size", ".95rem")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message)
    }
}

@Composable
internal fun EightBallReelSwipeHint(text: String) {
    Column(
        modifier = Modifier
            .styleModifier {
                property("min-width", "96px")
                property("height", "64px")
                property("display", "flex")
                property("flex-direction", "column")
                property("align-items", "center")
                property("justify-content", "center")
                property("gap", "7px")
                property("animation", "eightBallReelPulse 1.15s ease-in-out infinite")
            }
    ) {
        FaAnglesUp(
            Modifier
                .styleModifier {
                    property("font-size", "1.9rem")
                    property("line-height", "1")
                    property("color", "rgba(235,247,238,.88)")
                    property("filter", "drop-shadow(0 8px 18px rgba(0,0,0,.68))")
                }
        )
        Span(
            attrs = Modifier
                .styleModifier {
                    property("color", "rgba(235,247,238,.76)")
                    property("font-size", ".78rem")
                    property("font-weight", "700")
                    property("letter-spacing", "0")
                    property("text-transform", "uppercase")
                    property("white-space", "nowrap")
                }
                .toAttrs()
        ) {
            Text(text)
        }
    }
}
