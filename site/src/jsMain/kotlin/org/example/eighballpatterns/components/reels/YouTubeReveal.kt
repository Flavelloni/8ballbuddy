package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.foundation.layout.Row
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.attrsModifier
import com.varabyte.kobweb.compose.ui.graphics.Color
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.borderRadius
import com.varabyte.kobweb.compose.ui.modifiers.color
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.fontSize
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.styleModifier
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.navigation.BasePath
import com.varabyte.kobweb.silk.components.text.SpanText
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.compose.foundation.layout.Box
import kotlinx.browser.document
import kotlinx.browser.window
import org.example.eighballpatterns.browser.YouTubeClipPlayer
import org.example.eighballpatterns.browser.youtubeEmbedUrl
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.YouTubeShotSegment
import org.example.eighballpatterns.model.eightBallReelResultText
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLIFrameElement

@Composable
internal fun YouTubeReveal(shot: EightBallShot, selectedBall: Int, segment: YouTubeShotSegment, onReset: () -> Unit) {
    var frame by remember { mutableStateOf<HTMLIFrameElement?>(null) }
    var controller by remember { mutableStateOf<YouTubeClipPlayer?>(null) }
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fullscreenUnavailable by remember { mutableStateOf(false) }
    var replayAttempt by remember { mutableStateOf(0) }
    DisposableEffect(frame, segment) {
        val bridge = frame?.let { iframe ->
            YouTubeClipPlayer(iframe, segment,
                onReady = { ready = true },
                onState = { state ->
                    playing = state == 1
                    if (state == 1) { blocked = false; ended = false }
                    if (state == 0) { playing = false; ended = true }
                },
                onBlocked = { blocked = true; playing = false },
                onError = { error = it; playing = false },
            ).also { controller = it; it.connect() }
        }
        onDispose {
            bridge?.dispose()
            if (controller === bridge) controller = null
        }
    }
    Column(
        Modifier.fillMaxSize().gap(12.px).padding(top = 12.px, bottom = 18.px)
            .color(Colors.White).styleModifier {
                property("box-sizing", "border-box")
                property("overflow-y", "auto")
                property("justify-content", "center")
                property("text-align", "center")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpanText(
            eightBallReelResultText(selectedBall, shot.meta),
            Modifier.fontSize(1.cssRem).color(if (selectedBall == shot.meta.correctAnswer) Color.rgb(0x94C9B5) else Color.rgb(0xFFC365)),
        )
        // YouTube owns this iframe, including removal on destroy. Compose owns only its host Box.
        // Never crop the player or place custom controls over it.
        val playerSize = Modifier.fillMaxWidth().styleModifier {
            // Full available width, with the whole 16:9 player visible. No cropping or CSS zoom.
            property("height", "max(200px, min(56.25vw, 258.75px))")
            property("min-width", "200px")
            property("border", "0")
            property("flex-shrink", "0")
        }
        if (ended) {
            // This REPLACES a destroyed player; it never covers a running YouTube iframe.
            Box(playerSize, contentAlignment = Alignment.Center) {
                SpanText("Shot finished", Modifier.fontSize(1.2.cssRem).color(Color.rgb(0x94C9B5)))
            }
        } else key(replayAttempt) {
            Box(playerSize.attrsModifier {
                ref { host ->
                    val iframe = document.createElement("iframe") as HTMLIFrameElement
                    iframe.src = youtubeEmbedUrl(segment, window.location.origin)
                    iframe.title = "YouTube shot reveal: ${shot.meta.player}"
                    iframe.setAttribute("allow", "autoplay; encrypted-media; fullscreen; picture-in-picture")
                    iframe.setAttribute("allowfullscreen", "")
                    iframe.setAttribute("referrerpolicy", "strict-origin-when-cross-origin")
                    iframe.style.cssText = "width:100%;height:100%;border:0;display:block"
                    host.appendChild(iframe)
                    frame = iframe
                    onDispose { if (frame === iframe) frame = null }
                }
            })
        }
        SpanText(
            when {
                error != null -> error!!
                blocked -> "Automatic playback was blocked. Tap Play shot."
                ended -> "Replay will show only this shot again."
                fullscreenUnavailable -> "Fullscreen is unavailable here. Rotate your phone for a larger view."
                !ready -> "Loading YouTube…"
                playing -> "Playing the original YouTube recording"
                else -> "Ready · YouTube may show ads or buffer before the shot"
            },
            Modifier.padding(leftRight = 14.px).fontSize(0.78.cssRem).color(Color.rgb(0xA9B5AF)).attrsModifier { attr("role", "status") },
        )
        Row(Modifier.gap(10.px), verticalAlignment = Alignment.CenterVertically) {
            YouTubeTestButton(if (ended) "Replay shot" else if (playing) "Pause shot" else "Play shot", ended || (ready && error == null)) {
                if (ended) {
                    ready = false
                    playing = false
                    blocked = false
                    error = null
                    replayAttempt += 1
                    ended = false
                }
                else if (playing) controller?.pause() else controller?.play()
            }
            YouTubeTestButton("Try again") { onReset() }
        }
        if (!ended && ready && jsTypeOf(frame?.asDynamic()?.requestFullscreen) == "function") {
            YouTubeTestButton("Fullscreen") {
                // Enlarge the complete player, never a clipped portion of it.
                frame?.asDynamic()?.requestFullscreen()?.catch { _: dynamic ->
                    fullscreenUnavailable = true
                }
            }
        }
        A(href = shot.meta.videoLink, attrs = Modifier.fontSize(0.8.cssRem).color(Color.rgb(0x94C9B5)).toAttrs {
            attr("target", "_blank"); attr("rel", "noopener noreferrer")
        }) { Text("Source: ${shot.meta.source} · Original video") }
        A(href = BasePath.prependTo("/?shot=${segment.shotId}&view=player"), attrs = Modifier.fontSize(0.8.cssRem).color(Color.rgb(0x94C9B5)).toAttrs()) {
            Text("Compare this shot with the current MP4 version")
        }
    }
}

@Composable
internal fun YouTubeTestButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = { onClick() }, enabled = enabled, modifier = Modifier.padding(leftRight = 12.px, topBottom = 9.px).borderRadius(20.px)
        .backgroundColor(Color.rgb(0x17362A)).color(Colors.White)
        .styleModifier { property("border", "1px solid #38594b"); property("cursor", "pointer") }
    ) { Text(label) }
}
