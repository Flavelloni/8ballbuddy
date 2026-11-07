package org.example.eighballpatterns.browser

import kotlinx.browser.document
import kotlinx.browser.window
import org.example.eighballpatterns.model.youtubeExperimentShot
import org.w3c.dom.HTMLIFrameElement
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YouTubePlayerTest {
    @Test
    fun embedHidesOnlySupportedControlsAndKeepsBoundedPlaybackAndIdentity() {
        val url = youtubeEmbedUrl(youtubeExperimentShot, "http://localhost:8080")
        assertTrue(url.startsWith("https://www.youtube.com/embed/AMDgls4nJhQ?"))
        assertTrue(url.contains("controls=0"))
        assertTrue(url.contains("disablekb=1"))
        assertTrue(url.contains("start=19300&end=19309"))
        assertTrue(url.contains("origin=http%3A%2F%2Flocalhost%3A8080"))
        assertFalse(url.contains("showinfo"))
        assertFalse(url.contains("modestbranding"))
    }

    @Test
    fun readyMutesAndAutomaticallyLoadsBoundedShotAndReplayRetainsEndTime(): Promise<Unit> {
        val previousApi = window.asDynamic().YT
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        document.body!!.appendChild(iframe)
        val loads = mutableListOf<Pair<Double, Double>>()
        var callbacks: dynamic = null
        var muted = false
        var destroyed = false
        val fake = js("({})")
        fake.mute = { muted = true }
        fake.loadVideoById = { options: dynamic ->
            assertEquals(youtubeExperimentShot.videoId, options.videoId)
            loads.add((options.startSeconds as Double) to (options.endSeconds as Double))
        }
        fake.getPlayerState = { 2 }
        fake.destroy = { destroyed = true; iframe.remove() }
        val api = js("({})")
        api.Player = { _: dynamic, options: dynamic -> callbacks = options.events; fake }
        window.asDynamic().YT = api
        var ready = false
        val errors = mutableListOf<String>()
        val player = YouTubeClipPlayer(iframe, youtubeExperimentShot, { ready = true }, {}, {}, { errors.add(it) })
        return Promise { resolve, reject ->
            player.connect()
            window.setTimeout({
                try {
                    val event = js("({})")
                    event.target = fake
                    callbacks.onReady(event)
                    assertTrue(ready)
                    assertTrue(muted)
                    player.replay()
                    assertEquals(listOf(19300.4 to 19308.5, 19300.4 to 19308.5), loads)
                    assertTrue(errors.isEmpty())
                    player.dispose()
                    assertTrue(destroyed)
                    assertFalse(iframe.isConnected)
                    resolve(Unit)
                } catch (error: Throwable) {
                    player.dispose()
                    reject(error)
                } finally { window.asDynamic().YT = previousApi; iframe.remove() }
            }, 0)
        }
    }

    @Test
    fun leavingBeforeApiIsReadyCannotCreateAnOrphanedPlayer(): Promise<Unit> {
        val previousApi = window.asDynamic().YT
        var created = false
        val api = js("({})")
        api.Player = { _: dynamic, _: dynamic -> created = true; js("({})") }
        window.asDynamic().YT = api
        val iframe = document.createElement("iframe") as HTMLIFrameElement
        val player = YouTubeClipPlayer(iframe, youtubeExperimentShot, {}, {}, {}, {})
        return Promise { resolve, reject ->
            player.connect()
            player.dispose()
            window.setTimeout({
                try { assertFalse(created); resolve(Unit) }
                catch (error: Throwable) { reject(error) }
                finally { window.asDynamic().YT = previousApi }
            }, 0)
        }
    }
}
