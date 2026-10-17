package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.icons.fa.FaAnglesUp
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

private const val EIGHT_BALL_WHEEL_THRESHOLD = 56.0
private const val EIGHT_BALL_WHEEL_GESTURE_GAP_MS = 500.0
private const val EIGHT_BALL_WHEEL_COOLDOWN_MS = 500.0

private class WheelGesture {
    private var accumulatedY = 0.0
    private var lastEventAt = 0.0
    private var lastNavigationAt = Double.NEGATIVE_INFINITY

    fun direction(deltaY: Double, timeStamp: Double): Int {
        if (timeStamp - lastEventAt > EIGHT_BALL_WHEEL_GESTURE_GAP_MS) accumulatedY = 0.0
        lastEventAt = timeStamp
        if (timeStamp - lastNavigationAt < EIGHT_BALL_WHEEL_COOLDOWN_MS) return 0
        accumulatedY += deltaY
        if (accumulatedY < EIGHT_BALL_WHEEL_THRESHOLD && accumulatedY > -EIGHT_BALL_WHEEL_THRESHOLD) return 0
        val direction = if (accumulatedY > 0) 1 else -1
        accumulatedY = 0.0
        lastNavigationAt = timeStamp
        return direction
    }
}

internal fun ensureEightBallReelsStyles() {
    if (document.getElementById("eight-ball-reels-styles") != null) return

    val style = document.createElement("style")
    style.setAttribute("id", "eight-ball-reels-styles")
    style.textContent = """
        @keyframes eightBallReelPulse {
          0%, 100% { opacity: .42; transform: translateY(7px); }
          50% { opacity: 1; transform: translateY(-7px); }
        }
        .eight-ball-reel-intro { gap: 24px; padding: 32px; }
        .eight-ball-reel-brand { gap: 16px; }
        .eight-ball-reel-logo { width: 128px; height: 128px; flex-shrink: 0; }
        @media (max-height: 650px) {
          .eight-ball-reel-intro { gap: 16px; padding: 20px; }
          .eight-ball-reel-brand { gap: 12px; }
          .eight-ball-reel-logo { width: 80px; height: 80px; }
        }
        @media (max-height: 440px) {
          .eight-ball-reel-intro { gap: 10px; padding: 12px; }
          .eight-ball-reel-brand { flex-direction: row; align-items: center; }
          .eight-ball-reel-logo { width: 48px; height: 48px; }
          .eight-ball-reel-actions { gap: 8px !important; }
          .eight-ball-reel-actions button { width: 42px !important; height: 42px !important; }
        }
    """.trimIndent() + "\n" + dailyChallengeStyles
    document.head?.appendChild(style)
}

@Composable
internal fun EightBallReelIntro(onStart: () -> Unit, canStart: Boolean = true) {
    Column(
        Modifier
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("justify-content", "center")
                property("box-sizing", "border-box")
                property("color", "rgba(246,250,246,.9)")
                property("text-align", "center")
            }
            .attrsModifier { classes("eight-ball-reel-intro") },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.attrsModifier { classes("eight-ball-reel-brand") },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Img(
                src = BasePath.prependTo("/images/pool-ball-icon.svg"),
                alt = "8-Ball Buddy logo",
                attrs = {
                    classes("eight-ball-reel-logo")
                    attr("width", "128")
                    attr("height", "128")
                    attr("draggable", "false")
                },
            )
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
                .styleModifier {
                    property("cursor", if (canStart) "pointer" else "wait")
                    property("opacity", if (canStart) "1" else ".5")
                }
                .toAttrs {
                    if (!canStart) attr("disabled", "")
                    onClick { onStart() }
                }
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
    allowContentScroll: Boolean = false,
    content: @Composable () -> Unit,
) {
    val wheelGesture = remember { WheelGesture() }
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
                property("touch-action", if (allowContentScroll) "pan-y" else "none")
            }
            .attrsModifier {
                onWheel { event ->
                    if (allowContentScroll) return@onWheel
                    event.preventDefault()
                    val deltaY = event.deltaY * when (event.deltaMode) {
                        1 -> 16.0
                        2 -> window.innerHeight.toDouble()
                        else -> 1.0
                    }
                    when (wheelGesture.direction(deltaY, window.performance.now())) {
                        1 -> onNext()
                        -1 -> onPrevious()
                    }
                }
                onTouchStart { event ->
                    if (allowContentScroll) return@onTouchStart
                    val touch = event.touches.item(0) ?: return@onTouchStart
                    onTouchStart(touch.clientY.toDouble())
                }
                onTouchEnd { event ->
                    if (allowContentScroll) return@onTouchEnd
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
