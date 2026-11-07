package org.example.eighballpatterns.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.minHeight
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.position
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.text.SpanText
import org.example.eighballpatterns.components.reels.EightBallFullscreenImage
import org.example.eighballpatterns.components.reels.EightBallPlayerIntro
import org.example.eighballpatterns.components.reels.EightBallReelLayoutOverlay
import org.example.eighballpatterns.components.reels.EightBallReelMessage
import org.example.eighballpatterns.components.reels.EightBallReelsShell
import org.example.eighballpatterns.components.reels.YouTubeReveal
import org.example.eighballpatterns.components.reels.YouTubeTestButton
import org.example.eighballpatterns.components.reels.ensureEightBallReelsStyles
import org.example.eighballpatterns.data.loadEightBallShots
import org.example.eighballpatterns.data.loadPlayerProfiles
import org.example.eighballpatterns.model.ENABLE_YOUTUBE_EXPERIMENT
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.PlayerProfile
import org.example.eighballpatterns.model.youtubeExperimentShot
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text

/** Opt-in single-shot experiment. No normal catalog, challenge or MP4 playback logic is changed. */
@Page("/youtube-test")
@Composable
fun YouTubeTestPage() {
    var shot by remember { mutableStateOf<EightBallShot?>(null) }
    var players by remember { mutableStateOf(emptyMap<String, PlayerProfile>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var stage by remember { mutableStateOf(0) } // Player intro -> layout -> reveal.
    var selectedBall by remember { mutableStateOf<Int?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var startY by remember { mutableStateOf<Double?>(null) }
    LaunchedEffect(Unit) {
        ensureEightBallReelsStyles()
        if (ENABLE_YOUTUBE_EXPERIMENT) {
            loadPlayerProfiles { players = it }
            loadEightBallShots(onLoaded = { shots ->
                shot = shots.find { it.entry.id == youtubeExperimentShot.shotId }
                if (shot == null) error = "The experiment's sample shot is unavailable."
            }, onError = { error = it })
        }
    }
    fun showLayout() { selectedBall = null; stage = 1 }
    fun next() {
        if (!fullscreen) when (stage) { 0 -> stage = 1; 2 -> showLayout() }
    }
    fun previous() {
        if (!fullscreen) { selectedBall = null; stage = if (stage == 2) 1 else 0 }
    }
    EightBallReelsShell(
        onNext = { next() }, onPrevious = { previous() },
        allowContentScroll = stage == 2,
        onTouchStart = { startY = it }, onTouchCancel = { startY = null },
        onTouchEnd = { y ->
            startY?.let { start -> if (y - start <= -64) next() else if (y - start >= 64) previous() }
            startY = null
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(leftRight = 14.px, topBottom = 10.px).gap(12.px)
                    .styleModifier { property("box-sizing", "border-box"); property("padding-top", "max(10px, env(safe-area-inset-top))") },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SpanText("YouTube test", Modifier.fontSize(0.8.cssRem).color(Color.rgb(0x94C9B5)).weight(1))
                A(href = BasePath.prependTo("/"), attrs = Modifier.fontSize(0.8.cssRem).color(Color.rgb(0x94C9B5)).toAttrs()) { Text("Back to 8ballbuddy") }
            }
            Box(Modifier.fillMaxWidth().weight(1).minHeight(0.px).position(Position.Relative)) {
                val current = shot
                when {
                    !ENABLE_YOUTUBE_EXPERIMENT -> EightBallReelMessage("The YouTube experiment is disabled.")
                    error != null -> EightBallReelMessage(error!!)
                    current == null -> EightBallReelMessage("Loading the sample shot…")
                    stage == 0 -> Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().weight(1).minHeight(0.px)) {
                            EightBallPlayerIntro(current, players[current.meta.player.trim().lowercase()])
                        }
                        Row(Modifier.fillMaxWidth().padding(bottom = 12.px), horizontalArrangement = com.varabyte.kobweb.compose.foundation.layout.Arrangement.Center) {
                            YouTubeTestButton("Show layout") { stage = 1 }
                        }
                    }
                    stage == 1 -> EightBallReelLayoutOverlay(
                        current, onImageFullscreen = { fullscreen = true },
                        onBallSelected = { ball -> if (selectedBall == null) { selectedBall = ball; stage = 2 } },
                        answerClock = null, inputEnabled = true, clockRunning = false,
                        onLayoutReady = {}, requestError = null, requestBusy = false, onRetry = {},
                    )
                    else -> key(selectedBall) {
                        YouTubeReveal(current, selectedBall!!, youtubeExperimentShot, onReset = { showLayout() })
                    }
                }
            }
        }
        if (fullscreen) EightBallFullscreenImage(shot, onClose = { fullscreen = false })
    }
}
