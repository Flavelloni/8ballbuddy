package org.example.eighballpatterns.browser

import com.varabyte.kobweb.navigation.BasePath
import kotlinx.browser.window
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.eightBallReelTitle

internal fun currentEightBallReelsShotId(): String? {
    return js("new URLSearchParams(window.location.search).get('shot')") as? String
}

internal fun currentEightBallReelsPlayerIntro(): Boolean {
    return js("new URLSearchParams(window.location.search).get('view') === 'player'") as Boolean
}

internal fun updateEightBallReelsUrl(shotId: String?, playerIntro: Boolean) {
    val baseUrl = "${window.location.origin}${BasePath.prependTo("/")}"
    val view = if (playerIntro) "&view=player" else ""
    val nextUrl = if (shotId == null) baseUrl else "$baseUrl?shot=${encodeUriComponent(shotId)}$view"
    js("window.history.replaceState(null, '', nextUrl)")
}

internal fun eightBallReelsShareUrl(shot: EightBallShot): String {
    return "${window.location.origin}${BasePath.prependTo("/")}?shot=${encodeUriComponent(shot.entry.id)}"
}

internal fun copyEightBallShareUrl(shot: EightBallShot) {
    val url = eightBallReelsShareUrl(shot)
    js("if (navigator.clipboard) { navigator.clipboard.writeText(url); }")
}

internal fun shareEightBallShotNative(shot: EightBallShot): Boolean {
    val title = eightBallReelTitle(shot.meta)
    val url = eightBallReelsShareUrl(shot)
    return js("""
        (function() {
            if (!navigator.share) return false;
            navigator.share({ title: title, text: title, url: url }).catch(function() {});
            return true;
        })()
    """) as Boolean
}

internal fun openEightBallWhatsAppShare(shot: EightBallShot) {
    val text = encodeUriComponent("${eightBallReelTitle(shot.meta)} ${eightBallReelsShareUrl(shot)}")
    window.open("https://wa.me/?text=$text", "_blank")
}

internal fun openEightBallEmailShare(shot: EightBallShot) {
    val subject = encodeUriComponent(eightBallReelTitle(shot.meta))
    val body = encodeUriComponent(eightBallReelsShareUrl(shot))
    js("window.location.href = 'mailto:?subject=' + subject + '&body=' + body")
}

private fun encodeUriComponent(value: String): String {
    return js("encodeURIComponent(value)") as String
}
