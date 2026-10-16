package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.css.FontWeight
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.border
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.flexWrap
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.fontWeight
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.lineHeight
import com.varabyte.kobweb.compose.ui.modifiers.margin
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.icons.fa.FaExpand
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import org.example.eighballpatterns.components.widgets.PoolBall
import org.example.eighballpatterns.model.EightBallReelStats
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.ballLabel
import org.example.eighballpatterns.model.eightBallReelPickerBalls
import org.example.eighballpatterns.model.eightBallReelTitle
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLVideoElement

@Composable
internal fun EightBallReelShot(
    shot: EightBallShot,
    selectedBall: Int?,
    feedback: String?,
    videoEnded: Boolean,
    videoPaused: Boolean,
    stats: EightBallReelStats,
    liked: Boolean,
    likesLabel: String,
    onImageFullscreen: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onBallSelected: (Int) -> Unit,
    onVideoElement: (HTMLVideoElement?) -> Unit,
    onVideoEnded: () -> Unit,
    onVideoPlay: () -> Unit,
    onVideoPause: () -> Unit,
    onVideoToggle: () -> Unit,
) {
    val hasPrediction = selectedBall != null
    val correct = selectedBall == shot.meta.ball

    Box(
        modifier = Modifier
            .position(Position.Relative)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("min-height", "100dvh")
                property("-webkit-tap-highlight-color", "transparent")
                property("-webkit-user-select", "none")
                property("user-select", "none")
            }
    ) {
        EightBallReelVideoLayer(
            shot = shot,
            correct = correct,
            selectedBall = selectedBall,
            feedback = feedback,
            showFeedback = hasPrediction,
            showSwipeHint = hasPrediction && videoEnded,
            videoPaused = videoPaused,
            onVideoElement = onVideoElement,
            onVideoEnded = onVideoEnded,
            onVideoPlay = onVideoPlay,
            onVideoPause = onVideoPause,
            onVideoToggle = onVideoToggle,
        )

        if (!hasPrediction) {
            EightBallReelLayoutOverlay(
                shot = shot,
                onImageFullscreen = onImageFullscreen,
                onBallSelected = onBallSelected,
            )
        }

        EightBallReelActions(
            stats = stats,
            liked = liked,
            likesLabel = likesLabel,
            onLike = onLike,
            onShare = onShare,
        )
    }
}

@Composable
private fun EightBallReelLayoutOverlay(
    shot: EightBallShot,
    onImageFullscreen: () -> Unit,
    onBallSelected: (Int) -> Unit,
) {
    var layoutReady by remember(shot.entry.id) { mutableStateOf(false) }

    SimpleGrid(
        numColumns = numColumns(1),
        modifier = Modifier
            .position(Position.Absolute)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("inset", "0")
                property("z-index", "2")
                property("display", "grid")
                property("grid-template-rows", "auto minmax(0, 1fr) auto")
                property("background", "#020303")
            }
    ) {
        Row(
            modifier = Modifier
                .position(Position.Relative)
                .styleModifier {
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("min-height", "84px")
                    property("padding", "max(12px, env(safe-area-inset-top)) 64px 8px")
                    property("box-sizing", "border-box")
                }
        ) {
            Column(
                Modifier.gap(4.px),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShotClock(shot.entry.id, running = layoutReady)
                P(
                    attrs = Modifier.margin(0.px)
                        .styleModifier {
                            property("color", "rgba(245,248,245,.76)")
                            property("font-size", ".84rem")
                            property("font-weight", "650")
                            property("line-height", "1.25")
                            property("text-align", "center")
                        }
                        .toAttrs()
                ) {
                    Text(eightBallReelTitle(shot.meta))
                }
            }

            Button(
                attrs = Modifier
                    .position(Position.Absolute)
                    .zIndex(4)
                    .styleModifier {
                        property("top", "max(10px, env(safe-area-inset-top))")
                        property("right", "14px")
                        property("width", "40px")
                        property("height", "40px")
                        property("padding", "0")
                        property("border", "1px solid rgba(255,255,255,.18)")
                        property("border-radius", "999px")
                        property("background", "rgba(0,0,0,.56)")
                        property("color", "white")
                        property("font-size", "20px")
                        property("cursor", "pointer")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                        property("backdrop-filter", "blur(8px)")
                    }
                    .toAttrs {
                        attr("aria-label", "View image fullscreen")
                        title("View image fullscreen")
                        onClick { onImageFullscreen() }
                    }
            ) {
                FaExpand()
            }
        }

        Row(
            modifier = Modifier
                .position(Position.Relative)
                .styleModifier {
                    property("min-height", "0")
                    property("container-type", "size")
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("overflow", "hidden")
                }
        ) {
            Row(
                modifier = Modifier
                    .position(Position.Relative)
                    .styleModifier {
                        property("width", "min(calc(100% - 28px), 38dvh, 56.25cqh, 460px)")
                        property("aspect-ratio", "9 / 16")
                        property("overflow", "hidden")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                    }
            ) {
                Img(
                    src = BasePath.prependTo(shot.entry.imagePath),
                    attrs = Modifier
                        .styleModifier {
                            property("width", "177.777%")
                            property("height", "auto")
                            property("max-width", "none")
                            property("max-height", "none")
                            property("transform", "rotate(90deg)")
                            property("user-select", "none")
                            property("-webkit-user-select", "none")
                            property("-webkit-user-drag", "none")
                        }
                        .toAttrs {
                            attr("alt", "Initial 8-ball layout")
                            attr("draggable", "false")
                            addEventListener("load") { layoutReady = true }
                        }
                )
            }
        }

        EightBallReelBallTray(
            balls = eightBallReelPickerBalls(shot),
            onBallSelected = onBallSelected,
        )
    }
}

@Composable
private fun EightBallReelBallTray(balls: List<Int>, onBallSelected: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 12.px)
            .gap(0.45.cssRem)
            .styleModifier {
                property("box-sizing", "border-box")
                property("padding-bottom", "max(10px, env(safe-area-inset-bottom))")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Span(
            attrs = Modifier
                .fontSize(0.72.cssRem)
                .fontWeight(FontWeight.Bold)
                .lineHeight(1)
                .color(Color.rgba(235, 247, 238, 0.72f))
                .styleModifier { property("text-transform", "uppercase") }
                .toAttrs()
        ) {
            Text("Pick the ball the pro pots next")
        }
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.46.cssRem)
                .flexWrap(FlexWrap.Wrap)
                .styleModifier { property("justify-content", "center") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            balls.forEach { ball ->
                Button(
                    attrs = Modifier
                        .width(44.px)
                        .height(44.px)
                        .padding(5.px)
                        .borderRadius(999.px)
                        .backgroundColor(Color.rgba(255, 255, 255, 0.08f))
                        .border(1.px, LineStyle.Solid, Color.rgba(255, 255, 255, 0.18f))
                        .styleModifier {
                            property("box-sizing", "border-box")
                            property("cursor", "pointer")
                        }
                        .toAttrs {
                            title("${ballLabel(ball)} ball")
                            attr("aria-label", "${ballLabel(ball)} ball")
                            onClick { onBallSelected(ball) }
                        }
                ) {
                    PoolBall(ball, Modifier.width(100.percent).height(100.percent), fillParent = true)
                }
            }
        }
    }
}
