package org.example.eighballpatterns.data

import com.varabyte.kobweb.navigation.BasePath
import org.example.eighballpatterns.model.EightBallEntry
import org.example.eighballpatterns.model.EightBallMeta
import org.example.eighballpatterns.model.EightBallShot

internal fun loadEightBallShots(onLoaded: (List<EightBallShot>) -> Unit, onError: (String) -> Unit) {
    fetchResourceText(
        BasePath.prependTo("/8ball/index.json"),
        revalidate = true,
        onSuccess = { manifestText ->
            val entries = parseEightBallEntries(manifestText)
            if (entries.isEmpty()) {
                onLoaded(emptyList())
                return@fetchResourceText
            }

            val loaded = mutableListOf<EightBallShot>()
            var remaining = entries.size
            var failed = false

            entries.forEach { entry ->
                fetchResourceText(
                    BasePath.prependTo(entry.metaPath),
                    onSuccess = { metaText ->
                        if (failed) return@fetchResourceText
                        loaded += EightBallShot(entry, parseEightBallMeta(metaText))
                        remaining -= 1
                        if (remaining == 0) {
                            onLoaded(loaded.sortedBy { it.entry.id })
                        }
                    },
                    onError = {
                        if (!failed) {
                            failed = true
                            onError("Could not load ${entry.metaPath}: $it")
                        }
                    },
                )
            }
        },
        onError = { onError("Could not load 8-ball manifest: $it") },
    )
}

private fun parseEightBallEntries(json: String): List<EightBallEntry> {
    val items = js("JSON.parse(json)")
    val length = items.length as Int
    return (0 until length).map { index ->
        val item = items[index]
        EightBallEntry(
            id = item.id as String,
            metaPath = item.metaPath as String,
            imagePath = item.imagePath as String,
            videoPath = item.videoPath as String,
        )
    }
}

internal fun parseEightBallMeta(text: String): EightBallMeta {
    val item = js("JSON.parse(text)")
    return EightBallMeta(
        player = (item.player as? String).orEmpty(),
        opponent = (item.opponent as? String).orEmpty(),
        group = (item.group as? Int) ?: 0,
        ball = (item.ball as? Int) ?: -1,
        comments = (item.comments as? String).orEmpty(),
        source = (item.source as? String).orEmpty(),
        videoLink = (item.videoLink as? String).orEmpty(),
        isBreak = (item.isBreak as? Boolean) ?: false,
        ballsPotted = (item.ballsPotted as? Int),
        archived = (item.archived as? Boolean) ?: false,
        ballColors = parseBallColors(item.ballColors),
        browseOrder = (item.browseOrder as? Int) ?: 0,
    )
}

// Only numbered balls and six-digit RGB colors are accepted, never arbitrary CSS.
private fun parseBallColors(value: dynamic): Map<Int, Int> {
    if (!(js("typeof value === 'object' && value !== null && !Array.isArray(value)") as Boolean)) return emptyMap()
    val keys = js("Object.keys(value)") as Array<String>
    return keys.mapNotNull { key ->
        val number = key.toIntOrNull()
        val hex = value[key] as? String
        if (number == null || number !in 1..15 || key != number.toString() ||
            hex == null || !Regex("#[0-9a-fA-F]{6}").matches(hex)) null
        else number to hex.drop(1).toInt(16)
    }.toMap()
}
