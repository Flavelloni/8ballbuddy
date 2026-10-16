package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.height
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.modifiers.width
import com.varabyte.kobweb.compose.ui.modifiers.zIndex
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.icons.fa.FaPause
import com.varabyte.kobweb.silk.components.icons.fa.FaPlay
import com.varabyte.kobweb.silk.components.icons.fa.FaRotateRight
import com.varabyte.kobweb.silk.components.icons.fa.FaThumbsDown
import com.varabyte.kobweb.silk.components.icons.fa.FaThumbsUp
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.eightBallReelResultText
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Video
import org.w3c.dom.HTMLVideoElement

private const val EIGHT_BALL_REEL_VIDEO_SIDE_CROP_PERCENT = 22.0
private const val EIGHT_BALL_REEL_VIDEO_ZOOM = 1.05
private const val EIGHT_BALL_REEL_VIDEO_SOURCE_ASPECT = 16.0 / 9.0
private const val EIGHT_BALL_REEL_VIDEO_AVAILABLE_HEIGHT_PX = 260.0

@Composable
internal fun EightBallReelVideoLayer(
    shot: EightBallShot,
    correct: Boolean,
    selectedBall: Int?,
    feedback: String?,
    showFeedback: Boolean,
    showSwipeHint: Boolean,
    videoPaused: Boolean,
    onVideoElement: (HTMLVideoElement?) -> Unit,
    onVideoEnded: () -> Unit,
    onVideoPlay: () -> Unit,
    onVideoPause: () -> Unit,
    onVideoToggle: () -> Unit,
) {
    SimpleGrid(
        numColumns = numColumns(1),
        modifier = Modifier
            .position(Position.Absolute)
            .width(100.percent)
            .height(100.percent)
            .styleModifier {
                property("inset", "0")
                property("display", "grid")
                property("grid-template-rows", "112px minmax(0, 1fr) 112px")
                property("background", "#020303")
            }
    ) {
        Column(
            modifier = Modifier
                .styleModifier {
                    property("display", "flex")
                    property("flex-direction", "column")
                    property("align-items", "center")
                    property("justify-content", "end")
                    property("gap", "4px")
                    property("padding", "max(14px, env(safe-area-inset-top)) 18px 10px")
                    property("opacity", if (showFeedback) "1" else "0")
                }
        ) {
            Span(
                attrs = Modifier
                    .styleModifier {
                        property("width", "46px")
                        property("height", "46px")
                        property("border-radius", "999px")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                        property("font-size", "1.45rem")
                        property("background", if (correct) "rgba(31, 151, 78, .24)" else "rgba(203, 50, 50, .23)")
                        property("border", if (correct) "1px solid rgba(102, 232, 151, .45)" else "1px solid rgba(255, 115, 115, .45)")
                        property("color", if (correct) "#79e69c" else "#ff7373")
                    }
                    .toAttrs()
            ) {
                if (correct) {
                    FaThumbsUp()
                } else {
                    FaThumbsDown()
                }
            }
            Span(
                attrs = Modifier
                    .styleModifier {
                        property("color", if (correct) "rgba(126,218,116,.92)" else "rgba(255,195,101,.92)")
                        property("font-size", ".88rem")
                        property("font-weight", "750")
                        property("text-align", "center")
                        property("line-height", "1.25")
                    }
                    .toAttrs()
            ) {
                Text(eightBallReelResultText(selectedBall, shot.meta.ball))
            }
            if (feedback != null) {
                Span(
                    attrs = Modifier
                        .styleModifier {
                            property("color", "rgba(245,248,245,.62)")
                            property("font-size", ".78rem")
                            property("font-weight", "600")
                            property("text-align", "center")
                            property("line-height", "1.25")
                        }
                        .toAttrs()
                ) {
                    Text(feedback)
                }
            }
        }

        Row(
            modifier = Modifier
                .styleModifier {
                    property("display", "flex")
                    property("align-items", "center")
                    property("justify-content", "center")
                    property("min-height", "0")
                    property("overflow", "hidden")
                }
        ) {
            Box(
                modifier = Modifier
                    .position(Position.Relative)
                    .styleModifier {
                        property("width", eightBallReelVideoFrameWidth())
                        property("aspect-ratio", eightBallReelVideoFrameAspect().toString())
                        property("overflow", "hidden")
                        property("background", "#020303")
                    }
            ) {
                Video(
                    attrs = Modifier
                        .styleModifier {
                            property("width", "100%")
                            property("height", "100%")
                            property("max-width", "none")
                            property("max-height", "none")
                            property("object-fit", "cover")
                            property("transform", "scale($EIGHT_BALL_REEL_VIDEO_ZOOM)")
                            property("transform-origin", "center center")
                            property("background", "#020303")
                            property("outline", "none")
                        }
                        .toAttrs {
                            attr("src", BasePath.prependTo(shot.entry.videoPath))
                            attr("poster", BasePath.prependTo(shot.entry.imagePath))
                            attr("playsinline", "")
                            attr("disablepictureinpicture", "")
                            attr("preload", "auto")
                            attr("tabindex", "-1")
                            ref { element ->
                                onVideoElement(element)
                                onDispose { onVideoElement(null) }
                            }
                            addEventListener("ended") { onVideoEnded() }
                            addEventListener("play") { onVideoPlay() }
                            addEventListener("pause") { onVideoPause() }
                        }
                )
                if (showFeedback) {
                    EightBallReelVideoControl(
                        videoEnded = showSwipeHint,
                        videoPaused = videoPaused,
                        onClick = onVideoToggle,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .styleModifier {
                    property("display", "flex")
                    property("align-items", "start")
                    property("justify-content", "center")
                    property("padding", "12px 18px max(18px, env(safe-area-inset-bottom))")
                    property("opacity", if (showSwipeHint) "1" else "0")
                    property("pointer-events", "none")
                }
        ) {
            EightBallReelSwipeHint("Swipe next")
        }
    }
}

@Composable
private fun EightBallReelVideoControl(
    videoEnded: Boolean,
    videoPaused: Boolean,
    onClick: () -> Unit,
) {
    val label = when {
        videoEnded -> "Replay shot"
        videoPaused -> "Play shot"
        else -> "Pause shot"
    }

    Button(
        attrs = Modifier
            .position(Position.Absolute)
            .zIndex(3)
            .styleModifier {
                property("left", "12px")
                property("bottom", "12px")
                property("width", "42px")
                property("height", "42px")
                property("padding", "0")
                property("border", "1px solid rgba(255,255,255,.18)")
                property("border-radius", "999px")
                property("background", "rgba(0,0,0,.48)")
                property("color", "white")
                property("font-size", "1rem")
                property("cursor", "pointer")
                property("display", "flex")
                property("align-items", "center")
                property("justify-content", "center")
                property("backdrop-filter", "blur(8px)")
            }
            .toAttrs {
                attr("aria-label", label)
                title(label)
                onClick { onClick() }
            }
    ) {
        when {
            videoEnded -> FaRotateRight()
            videoPaused -> FaPlay()
            else -> FaPause()
        }
    }
}

private fun eightBallReelVideoFrameAspect(): Double {
    val visibleWidth = 1.0 - EIGHT_BALL_REEL_VIDEO_SIDE_CROP_PERCENT / 50.0
    return EIGHT_BALL_REEL_VIDEO_SOURCE_ASPECT * EIGHT_BALL_REEL_VIDEO_ZOOM * visibleWidth
}

private fun eightBallReelVideoFrameWidth(): String {
    val aspect = eightBallReelVideoFrameAspect()
    val heightLimitedDvh = aspect * 100.0
    val heightLimitedPx = aspect * EIGHT_BALL_REEL_VIDEO_AVAILABLE_HEIGHT_PX
    return "min(calc(100% - 28px), calc(${heightLimitedDvh}dvh - ${heightLimitedPx}px), 460px)"
}
