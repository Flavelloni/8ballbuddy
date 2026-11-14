package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.css.TouchAction
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.touchAction
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.icons.fa.FaAnglesUp
import com.varabyte.kobweb.silk.components.icons.fa.FaArrowUp
import com.varabyte.kobweb.silk.components.icons.fa.FaArrowDown
import com.varabyte.kobweb.silk.components.icons.fa.FaChevronDown
import com.varabyte.kobweb.silk.components.forms.Button as SilkButton
import kotlinx.browser.document
import kotlinx.browser.window
import org.example.eighballpatterns.browser.installButtonFeedback
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.events.Event

// VisualViewport is not exposed by the project's Kotlin DOM bindings.
private fun reelViewportScale(): Double = (window.asDynamic().visualViewport?.scale as? Double) ?: 1.0

internal fun ensureEightBallReelsStyles() {
    if (document.getElementById("eight-ball-reels-styles") != null) return

    val style = document.createElement("style")
    style.setAttribute("id", "eight-ball-reels-styles")
    style.textContent = """
        @keyframes poolLoading { 0%, 75%, 100% { opacity: .15; } 30%, 60% { opacity: 1; } }
        .pool-loading-ball { animation: poolLoading 1.4s infinite; }
        .player-choice-card.player-choice-card:hover { background: #203f32; border-color: #94c9b5; }
        .player-choice-card:focus-visible { outline: 2px solid #94c9b5; outline-offset: 3px; }
        .layout-ball-button:focus-visible { outline: 2px solid #b7ffe0; outline-offset: 2px; }
        @keyframes ballSelectionPulse { from { transform: translate(-50%,-50%) scale(.85); opacity: 1; } to { transform: translate(-50%,-50%) scale(2.5); opacity: 0; } }
        .ball-selection-pulse { animation: ballSelectionPulse .48s ease-out both; }
        @keyframes layoutArrivalPulse {
          0%, 100% { transform: translate(-50%, -50%) scale(1); }
          50% { transform: translate(-50%, -50%) scale(var(--layout-pulse-scale, 1.5)); }
        }
        .layout-arrival-pulse { animation: layoutArrivalPulse 10s ease-in-out 1; }
        @media (prefers-reduced-motion: reduce) { .layout-arrival-pulse { animation: none; } }
        @keyframes predictionPop { 0% { transform: scale(.45); opacity: 0; } 45% { transform: scale(1.12); opacity: 1; } 70%,100% { transform: scale(1); opacity: 1; } }
        .prediction-pop { animation: predictionPop .6s cubic-bezier(.2,.8,.2,1) both; }
        .desktop-reel-navigation { display: none; }
        @media (min-width: 760px) and (hover: hover) and (pointer: fine) {
          .desktop-reel-navigation { display: flex; position: absolute; top: 50%; left: calc(50% + min(50vw, 340px) + 20px); transform: translateY(-50%); gap: 18px; z-index: 20; }
        }
        .desktop-reel-navigation button { width: 54px; height: 54px; padding: 0; border: 1px solid #52735f; border-radius: 50%; color: #daffea; background: radial-gradient(circle at 30% 20%, #416350, #102219 75%); box-shadow: 0 6px 18px #0009, inset 0 1px 2px #ffffff33; }
        .desktop-reel-navigation button:hover { border-color: #b7ffe0; }
        .player-more-details { max-height: 180px; overflow-y: auto; }
        @media (prefers-reduced-motion: reduce) { .ball-selection-pulse { display: none; } .prediction-pop { animation: none; } }
        @media (prefers-reduced-motion: reduce) { .pool-loading-ball { animation: none; } }
        .eight-ball-reel-intro { gap: 24px; padding: 32px; }
        .eight-ball-reel-brand { gap: 16px; }
        .eight-ball-reel-logo { width: 128px; height: 128px; flex-shrink: 0; }
        @container eight-ball-ball-tray (max-width: calc(308px + 2.76rem)) {
          .eight-ball-seven-picker { max-width: calc(177px + 1.38rem); }
        }
        @media (max-height: 650px) {
          .eight-ball-reel-intro { gap: 16px; padding: 20px; }
          .eight-ball-reel-brand { gap: 12px; }
          .eight-ball-reel-logo { width: 80px; height: 80px; }
        }
        @media (max-height: 440px) {
          .eight-ball-reel-intro { gap: 10px; padding: 12px; }
          .eight-ball-reel-brand { flex-direction: row; align-items: center; }
          .eight-ball-reel-logo { width: 48px; height: 48px; }
        }
    """.trimIndent() + "\n" + dailyChallengeStyles + "\n" + reelDesignStyles
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
        P(
            attrs = Modifier
                .margin(0.px)
                .color(Color.rgb(0xADE7CB))
                .styleModifier {
                    property("max-width", "320px")
                    property("font-size", "1.16rem")
                    property("font-weight", "750")
                    property("line-height", "1.32")
                }
                .attrsModifier { classes("reel-hero-slogan") }.toAttrs()
        ) {
            Text("Do you think like an expert?")
        }
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
                    .lineHeight(1.05)
                    .fontWeight(FontWeight.Bold)
                    .color(Colors.White)
                    .attrsModifier {classes("reel-hero-title")}.toAttrs()
            ) {
                Text("8-Ball Buddy")
            }
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
                .attrsModifier { classes("reel-hero-copy") }.toAttrs()
        ) {
            Text("Real matches. Your prediction. Tap a ball on the table, then watch the shot unfold.")
        }

        SilkButton(onClick={onStart()},enabled=canStart,modifier=Modifier
            .attrsModifier { classes("reel-start-cue");attr("aria-label","Scroll to start") }) {
            com.varabyte.kobweb.silk.components.text.SpanText("Scroll to start",Modifier.attrsModifier {classes("reel-scroll-label")})
            com.varabyte.kobweb.silk.components.text.SpanText("Swipe up to start",Modifier.attrsModifier {classes("reel-touch-label")})
            FaChevronDown()
        }
        InstallAppButton()
    }
}

@Composable
internal fun EightBallReelsShell(
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    pager: ReelPager,
    pageKey: Any,
    previousKey: Any?,
    nextKey: Any?,
    preview: @Composable (Any) -> Unit,
    onSettled: (Int) -> Unit,
    scrollLocked: Boolean = false,
    canNext: Boolean = true,
    canPrevious: Boolean = true,
    onHome: (() -> Unit)? = null,
    homeEnabled: Boolean = true,
    showSound: Boolean = false,
    content: @Composable () -> Unit,
) {
    var viewportScale by remember { mutableStateOf(reelViewportScale()) }
    DisposableEffect(Unit) {
        val viewport = window.asDynamic().visualViewport
        val onResize: (Event) -> Unit = { viewportScale = reelViewportScale(); pager.resize() }
        viewport?.addEventListener("resize", onResize)
        window.addEventListener("resize", onResize)
        onDispose {
            viewport?.removeEventListener("resize", onResize)
            window.removeEventListener("resize", onResize)
        }
    }
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .position(Position.Relative)
            .width(100.percent)
            .touchAction(TouchAction.PanY)
            .styleModifier {
                property("height", "100dvh")
                property("min-height", "100dvh")
                property("background", "radial-gradient(circle at 50% 18%, rgba(32,64,49,.32), transparent 36%), #050606")
                property("overflow", "hidden")
            }
            .attrsModifier {
                classes("eight-ball-app")
                ref { root ->
                    val disposeFeedback = installButtonFeedback(root)
                    onDispose { disposeFeedback() }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .position(Position.Relative)
                .height(100.percent)
                .styleModifier {
                    property("width", "min(100vw, 680px)")
                    property("min-height", "100dvh")
                    property("overflow", "hidden")
                    property("box-shadow", "0 0 80px rgba(0,0,0,.58)")
                }.attrsModifier { classes("reel-canvas") }
        ) {
            ReelScrollPages(pager,pageKey,previousKey,nextKey,
                scrollLocked || isReelViewportZoomed(viewportScale),onSettled,preview,content)
            onHome?.let { ReelBrandHeader(it,homeEnabled,showSound) }
        }
        if(nextKey != null || previousKey != null) Column(Modifier.attrsModifier { classes("desktop-reel-navigation");attr("aria-label","Reel navigation") }) {
            SilkButton(onClick={onPrevious()},enabled=canPrevious,modifier=Modifier.attrsModifier {attr("aria-label","Previous reel page");attr("title","Previous reel page")}) { FaArrowUp() }
            SilkButton(onClick={onNext()},enabled=canNext,modifier=Modifier.attrsModifier {attr("aria-label","Next reel page");attr("title","Next reel page")}) { FaArrowDown() }
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
                property("flex-direction", "column")
                property("align-items", "center")
                property("justify-content", "center")
                property("gap", "7px")
            }.attrsModifier {classes("reel-swipe-hint")}
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
