package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.varabyte.kobweb.compose.ui.attrsModifier
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
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.layout.SimpleGrid
import com.varabyte.kobweb.silk.components.layout.numColumns
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.components.widgets.PoolBall
import org.example.eighballpatterns.model.EightBallReelStats
import org.example.eighballpatterns.model.AnswerClock
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.ImagePoint
import org.example.eighballpatterns.model.PlayerProfile
import com.varabyte.kobweb.silk.components.graphics.Image
import com.varabyte.kobweb.silk.components.icons.fa.FaUser
import org.example.eighballpatterns.model.ballLabel
import org.example.eighballpatterns.model.eightBallReelPickerBalls
import org.jetbrains.compose.web.css.FlexWrap
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLVideoElement

@Composable
internal fun EightBallReelShot(
    shot: EightBallShot,
    player: PlayerProfile? = null,
    selectedBall: Int?,
    feedback: String?,
    videoEnded: Boolean,
    videoPaused: Boolean,
    stats: EightBallReelStats,
    liked: Boolean,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onBallSelected: (Int) -> Unit,
    onVideoElement: (HTMLVideoElement?) -> Unit,
    onVideoEnded: () -> Unit,
    onVideoPlay: () -> Unit,
    onVideoPause: () -> Unit,
    onVideoToggle: () -> Unit,
    answerClock: AnswerClock? = null,
    onChallengeNext: (() -> Unit)? = null,
    challengeLastShot: Boolean = false,
    inputEnabled: Boolean = true,
    clockRunning: Boolean = true,
    onLayoutReady: () -> Unit = {},
    requestError: String? = null,
    requestBusy: Boolean = false,
    onRetry: () -> Unit = {},
    pointSelection: ImagePoint? = null,
    coordinateResult: Boolean? = null,
    onPointSelected: ((ImagePoint) -> Unit)? = null,
    onPointConfirmed: (() -> Unit)? = null,
) {
    val hasPrediction = selectedBall != null
    val correct = coordinateResult ?: (selectedBall == shot.meta.correctAnswer)

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
            onChallengeNext = onChallengeNext,
            challengeLastShot = challengeLastShot,
            coordinateResult = coordinateResult,
        )

        if (!hasPrediction) {
            EightBallReelLayoutOverlay(
                shot = shot,
                player = player,
                pulseOnReady = true,
                onBallSelected = onBallSelected,
                answerClock = answerClock,
                inputEnabled = inputEnabled,
                clockRunning = clockRunning,
                onLayoutReady = onLayoutReady,
                requestError = requestError,
                requestBusy = requestBusy,
                onRetry = onRetry,
                pointSelection = pointSelection,
                onPointSelected = onPointSelected,
                onPointConfirmed = onPointConfirmed,
            )
        }

        EightBallReelActions(
            stats = stats,
            liked = liked,
            onLike = onLike,
            onShare = onShare,
        )
    }
}

@Composable
internal fun EightBallReelLayoutOverlay(
    shot: EightBallShot,
    player: PlayerProfile? = null,
    onBallSelected: (Int) -> Unit,
    answerClock: AnswerClock?,
    inputEnabled: Boolean,
    clockRunning: Boolean,
    onLayoutReady: () -> Unit,
    requestError: String?,
    requestBusy: Boolean,
    onRetry: () -> Unit,
    pointSelection: ImagePoint? = null,
    onPointSelected: ((ImagePoint) -> Unit)? = null,
    onPointConfirmed: (() -> Unit)? = null,
    pulseOnReady: Boolean = false,
) {
    val imageTarget=shot.meta.ballSelection?.imageTarget ?: shot.meta.target
    val coordinatePicking = !shot.meta.isBreak && imageTarget != null && onPointSelected != null && onPointConfirmed != null
    var layoutReady by remember(shot.entry.id) { mutableStateOf(isLayoutImageLoaded(shot.entry.imagePath)) }
    LaunchedEffect(layoutReady) {
        if (layoutReady) onLayoutReady()
    }

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
                property("background", "#080f0c")
            }.attrsModifier {classes("shot-layout-overlay")}
    ) {
        SimpleGrid(
            numColumns = numColumns(3),
            modifier = Modifier
                .position(Position.Relative)
                .styleModifier {
                    property("display", "grid")
                    property("grid-template-columns", "44px minmax(0, 1fr) 44px")
                    property("align-items", "center")
                    property("justify-content", "space-between")
                    property("gap", "10px")
                    property("padding", if (answerClock != null) "calc(98px + env(safe-area-inset-top)) 12px 8px" else "calc(66px + env(safe-area-inset-top)) 12px 8px")
                    property("box-sizing", "border-box")
                }
        ) {
            Box(Modifier.attrsModifier { classes("shot-player-avatar") },contentAlignment=Alignment.Center) {
                var failed by remember(player?.imageUrl) { mutableStateOf(false) }
                val image=player?.imageUrl?.takeIf {it.isNotBlank() && !failed}
                if(image==null) FaUser() else Image(
                    if(image.startsWith("http")) image else BasePath.prependTo(image),"${shot.meta.player} portrait",
                    Modifier.attrsModifier { addEventListener("error") { failed=true } })
            }
            Column(
                Modifier.gap(3.px).styleModifier {property("min-width","0")}.attrsModifier {classes("shot-heading")},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                P(
                    attrs = Modifier.margin(0.px)
                        .styleModifier {
                            property("color", "#e8f2ea")
                            property("font-size", ".92rem")
                            property("font-weight", "700")
                            property("line-height", "1.25")
                            property("text-align", "center")
                        }
                        .toAttrs()
                ) {
                    Text(shot.meta.player)
                }
                SpanText(org.example.eighballpatterns.model.eightBallPlayerStatus(shot.meta),
                    Modifier.fontSize(.74.cssRem).color(Color.rgb(0x9FBEAA)))
                if (shot.meta.ballInHand) {
                    SpanText("Ball in hand", Modifier.fontSize(0.9.cssRem).fontWeight(FontWeight.Bold)
                        .color(Color.rgb(0x94C9B5)))
                }
                if (answerClock != null) {
                    ShotClock(running = layoutReady && clockRunning, answerClock = answerClock)
                }
                if (requestBusy || requestError != null) {
                    EightBallChallengeRequestStatus(requestBusy, requestError, onRetry,
                        if (clockRunning) "Saving guess…" else "Starting shot clock…")
                }
            }
            Box { }
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
            if (!layoutReady) {
                // Loading must not change header height or resize the table when it disappears.
                Box(Modifier.position(Position.Absolute).styleModifier {
                    property("top", "8px")
                    property("left", "0")
                    property("right", "0")
                    property("z-index", "3")
                    property("pointer-events", "none")
                }, contentAlignment = Alignment.Center) { PoolLoading("Loading table…") }
            }
            Row(
                modifier = Modifier
                    .position(Position.Relative)
                    .styleModifier {
                        property("width", "100%")
                        property("height", "100%")
                        property("min-height", "0")
                        property("overflow", "hidden")
                        property("display", "flex")
                        property("align-items", "center")
                        property("justify-content", "center")
                    }
            ) {
                if (coordinatePicking) {
                    CoordinateLayout(shot.entry.imagePath, imageTarget!!, pointSelection,
                        inputEnabled, { onPointSelected?.invoke(it) }, { layoutReady = true }, shot.meta.ballSelection,
                        pulseOnReady = pulseOnReady)
                } else ZoomableLayout(shot.entry.imagePath, onReady = { layoutReady = true }, pulseOnReady = pulseOnReady)
            }
        }

        if (coordinatePicking) {
            CoordinateConfirm(pointSelection, layoutReady && inputEnabled, ballMode=shot.meta.ballSelection!=null) { onPointConfirmed?.invoke() }
        } else EightBallReelBallTray(
            balls = eightBallReelPickerBalls(shot),
            isBreak = shot.meta.isBreak,
            ballColors = shot.meta.ballColors,
            onBallSelected = onBallSelected,
            enabled = layoutReady && inputEnabled,
        )
    }
}

@Composable
private fun EightBallReelBallTray(balls: List<Int>, isBreak: Boolean, ballColors: Map<Int, Int>, onBallSelected: (Int) -> Unit, enabled: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(leftRight = 12.px)
            .gap(0.45.cssRem)
            .styleModifier {
                property("box-sizing", "border-box")
                property("padding-bottom", "max(10px, env(safe-area-inset-bottom))")
                property("container-type", "inline-size")
                property("container-name", "eight-ball-ball-tray")
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
            Text(if (isBreak) "How many balls will drop on the break?" else "Which ball will the expert pot?")
        }
        Row(
            Modifier
                .fillMaxWidth()
                .gap(0.46.cssRem)
                .flexWrap(FlexWrap.Wrap)
                .styleModifier { property("justify-content", "center") }
                .attrsModifier { if (balls.size == 7) classes("eight-ball-seven-picker") },
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
                            if (isBreak) {
                                property("color", "white")
                                property("font-size", "1.2rem")
                                property("font-weight", "750")
                            }
                        }
                        .toAttrs {
                            val label = if (isBreak) "$ball ${if (ball == 1) "ball" else "balls"} potted" else "${ballLabel(ball)} ball"
                            title(label)
                            attr("aria-label", label)
                            if (!enabled) attr("disabled", "")
                            onClick { onBallSelected(ball) }
                        }
                ) {
                    if (isBreak) Text(ball.toString())
                    else PoolBall(ball, Modifier.width(100.percent).height(100.percent), fillParent = true,
                        colorOverride = ballColors[ball]?.let { Color.rgb(it) })
                }
            }
        }
    }
}
