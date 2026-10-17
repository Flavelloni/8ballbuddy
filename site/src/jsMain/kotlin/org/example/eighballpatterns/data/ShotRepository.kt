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

private fun parseEightBallMeta(text: String): EightBallMeta {
    val item = js("JSON.parse(text)")
    return EightBallMeta(
        player = (item.player as? String).orEmpty(),
        opponent = (item.opponent as? String).orEmpty(),
        group = (item.group as? Int) ?: 0,
        ball = (item.ball as? Int) ?: -1,
        comments = (item.comments as? String).orEmpty(),
    )
}
