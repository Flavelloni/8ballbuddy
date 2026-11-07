package org.example.eighballpatterns.browser

import kotlinx.browser.document
import kotlinx.browser.window
import org.example.eighballpatterns.model.YouTubeShotSegment
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.HTMLScriptElement
import kotlin.js.Promise

private var youtubeApiLoading: Promise<Unit>? = null

internal fun youtubeEmbedUrl(segment: YouTubeShotSegment, origin: String): String {
    val encodedOrigin = js("encodeURIComponent(origin)") as String
    return "https://www.youtube.com/embed/${segment.videoId}?enablejsapi=1&playsinline=1" +
        "&controls=0&disablekb=1&rel=0&start=${segment.start.toInt()}" +
        "&end=${kotlin.math.ceil(segment.end).toInt()}&origin=$encodedOrigin"
}

private fun loadYouTubeApi(): Promise<Unit> {
    if (jsTypeOf(window.asDynamic().YT?.Player) == "function") return Promise.resolve(Unit)
    youtubeApiLoading?.let { return it }
    val loading = Promise<Unit> { resolve, reject ->
        val script = document.createElement("script") as HTMLScriptElement
        val previous = window.asDynamic().onYouTubeIframeAPIReady
        val timeout = window.setTimeout({
            script.remove()
            reject(IllegalStateException("YouTube did not load. Check your connection or content blocker."))
        }, 20000)
        window.asDynamic().onYouTubeIframeAPIReady = {
            window.clearTimeout(timeout)
            try {
                if (jsTypeOf(previous) == "function") previous()
            } finally {
                resolve(Unit)
            }
        }
        script.src = "https://www.youtube.com/iframe_api"
        script.onerror = { _, _, _, _, _ ->
            window.clearTimeout(timeout)
            script.remove()
            reject(IllegalStateException("YouTube was blocked or could not be reached."))
        }
        document.head!!.appendChild(script)
    }
    youtubeApiLoading = loading.catch { error ->
        youtubeApiLoading = null
        throw error
    }
    return youtubeApiLoading!!
}

/** Only the experimental route uses this bridge; normal HTML video playback is unchanged. */
internal class YouTubeClipPlayer(
    private val frame: HTMLIFrameElement,
    private val segment: YouTubeShotSegment,
    private val onReady: () -> Unit,
    private val onState: (Int) -> Unit,
    private val onBlocked: () -> Unit,
    private val onError: (String) -> Unit,
) {
    private var active = true
    private var ready = false
    private var player: dynamic = null
    private var poll: Int? = null
    private var startupTimeout: Int? = null

    private fun loadOptions(): dynamic {
        val options = js("({})")
        options.videoId = segment.videoId
        options.startSeconds = segment.start
        options.endSeconds = segment.end
        return options
    }

    fun connect() {
        startupTimeout = window.setTimeout({
            if (active && !ready) onError("YouTube is taking too long to respond. Try again or compare the MP4 version.")
        }, 25000)
        loadYouTubeApi().then {
            if (active) {
                val events = js("({})")
                events.onReady = { event: dynamic ->
                    if (active) {
                        ready = true
                        startupTimeout?.let(window::clearTimeout)
                        event.target.mute()
                        onReady()
                        // Replays also reload this bounded segment: seekTo() would discard endSeconds.
                        event.target.loadVideoById(loadOptions())
                    }
                }
                events.onStateChange = { event: dynamic -> if (active) onState(event.data as Int) }
                events.onAutoplayBlocked = { _: dynamic -> if (active) onBlocked() }
                events.onError = { event: dynamic ->
                    if (active) onError(when (event.data as Int) {
                        100 -> "The source video is unavailable."
                        101, 150 -> "The source owner has disabled embedded playback."
                        153 -> "YouTube could not identify this page. Open the test through the website, not an iframe URL."
                        else -> "YouTube could not play this video (error ${event.data})."
                    })
                }
                val options = js("({})")
                options.events = events
                val constructor = window.asDynamic().YT.Player
                val iframe = frame
                player = js("Reflect.construct(constructor, [iframe, options])")
                // Backup cutoff uses content time, never wall time: buffering/ads don't consume the shot.
                poll = window.setInterval({
                    if (active && ready && player.getPlayerState() == 1 && segment.hasEnded(player.getCurrentTime() as Double)) {
                        player.pauseVideo()
                        onState(0)
                    }
                }, 100)
            }
        }.catch { error -> if (active) onError(error.message ?: "Could not load YouTube.") }
    }

    fun play() { if (active && ready) player.playVideo() }
    fun pause() { if (active && ready) player.pauseVideo() }
    fun replay() { if (active && ready) player.loadVideoById(loadOptions()) }

    fun dispose() {
        active = false
        poll?.let(window::clearInterval)
        startupTimeout?.let(window::clearTimeout)
        player?.destroy()
        player = null
    }
}
