package org.example.eighballpatterns.browser

import kotlinx.browser.window

// Web Audio is not exposed by these Kotlin DOM bindings. Create/resume it in the
// confirming user gesture, before a daily challenge's asynchronous server reply.
internal fun primePredictionAudio() {
    try {
        js("""(function () {
            var Audio = window.AudioContext || window.webkitAudioContext;
            if (!Audio) return;
            if (!window.__poolAudio) window.__poolAudio = new Audio();
            if (window.__poolAudio.state === 'suspended') window.__poolAudio.resume().catch(function(){});
        })()""")
    } catch (_: Throwable) { }
}

internal fun predictionSoundEnabled(): Boolean = try {
    window.localStorage.getItem("prediction-sound") != "off"
} catch (_: Throwable) { true }

internal fun setPredictionSound(enabled: Boolean) {
    try { window.localStorage.setItem("prediction-sound", if (enabled) "on" else "off") } catch (_: Throwable) { }
}

internal fun playPredictionFeedback(correct: Boolean) {
    try {
        val navigator=window.navigator.asDynamic()
        if (js("typeof navigator.vibrate === 'function'") as Boolean)
            navigator.vibrate(if (correct) arrayOf(25,40,45) else arrayOf(65,35,65))
    } catch (_: Throwable) { }
    if (!predictionSoundEnabled()) return
    try {
        val audio=window.asDynamic().__poolAudio ?: return
        val start=audio.currentTime as Double
        val tones=if (correct) listOf(523.25,659.25,783.99) else listOf(311.13,233.08)
        tones.forEachIndexed { index, frequency ->
            val oscillator=audio.createOscillator()
            val gain=audio.createGain()
            val time=start+index*.11
            oscillator.type=if (correct) "sine" else "triangle"
            oscillator.frequency.setValueAtTime(frequency,time)
            gain.gain.setValueAtTime(0,time)
            gain.gain.linearRampToValueAtTime(.11,time+.015)
            gain.gain.exponentialRampToValueAtTime(.001,time+.16)
            oscillator.connect(gain);gain.connect(audio.destination)
            oscillator.start(time);oscillator.stop(time+.18)
            oscillator.onended={ gain.disconnect();oscillator.disconnect() }
        }
    } catch (_: Throwable) { } // Feedback must never block the reveal.
}
