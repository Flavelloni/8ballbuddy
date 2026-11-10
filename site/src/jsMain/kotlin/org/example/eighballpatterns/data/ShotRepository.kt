package org.example.eighballpatterns.data

import com.varabyte.kobweb.navigation.BasePath
import org.example.eighballpatterns.model.EightBallEntry
import org.example.eighballpatterns.model.EightBallMeta
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.BallTarget
import org.example.eighballpatterns.model.BallSelection
import org.example.eighballpatterns.model.LayoutBall

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
        // Explicit null means no pot; a missing/invalid value is still unknown.
        ball = if (item.ball == null && item.isBreak != true &&
            (js("Object.prototype.hasOwnProperty.call(item, 'ball')") as Boolean)) 0 else (item.ball as? Int) ?: -1,
        comments = (item.comments as? String).orEmpty(),
        source = (item.source as? String).orEmpty(),
        videoLink = (item.videoLink as? String).orEmpty(),
        isBreak = (item.isBreak as? Boolean) ?: false,
        ballsPotted = (item.ballsPotted as? Int),
        archived = (item.archived as? Boolean) ?: false,
        ballColors = parseBallColors(item.ballColors),
        browseOrder = (item.browseOrder as? Int) ?: 0,
        ballInHand = (item.ballInHand as? Boolean) ?: false,
        matchYear = (item.matchYear as? Int)?.takeIf { it in 1900..2100 },
        target = parseBallTarget(item.target),
        ballSelection = parseBallSelection(item.ballSelection),
    )
}

internal fun parseBallSelection(value: dynamic): BallSelection? {
    if (value == null || value.schemaVersion != 1 || !(js("Array.isArray(value.balls)") as Boolean)) return null
    val count=value.balls.length as Int
    if (count !in 1..32) return null
    val width=value.imageWidth as? Int ?: return null
    val height=value.imageHeight as? Int ?: return null
    val hash=value.imageSha256 as? String ?: return null
    val correct=value.correctBallId as? String ?: return null
    val number=value.correctNumber as? Int ?: return null
    if (width !in 1..20000 || height !in 1..20000 || number !in 1..15 || !Regex("[0-9a-f]{64}").matches(hash)) return null
    val balls=mutableListOf<LayoutBall>()
    for (i in 0 until count) {
        val b=value.balls[i]
        val id=b?.id as? String ?: return null
        val x=b.x as? Double ?: return null
        val y=b.y as? Double ?: return null
        val r=b.radius as? Double ?: return null
        if (id.isBlank() || id.length>40 || !x.isFinite() || !y.isFinite() || !r.isFinite() ||
            x !in 0.0..1.0 || y !in 0.0..1.0 || r !in .002..0.1 || balls.any { it.id==id }) return null
        balls.add(LayoutBall(id,x,y,r))
    }
    if (balls.none { it.id==correct }) return null
    return BallSelection(balls,correct,number,width,height,hash)
}

internal fun parseBallTarget(value: dynamic): BallTarget? {
    if (value == null) return null
    val x = value.x as? Double ?: return null
    val y = value.y as? Double ?: return null
    val radius = value.radius as? Double ?: return null
    val width = value.imageWidth as? Int ?: return null
    val height = value.imageHeight as? Int ?: return null
    val hash = value.imageSha256 as? String ?: return null
    if (!x.isFinite() || !y.isFinite() || !radius.isFinite() || x !in 0.0..1.0 || y !in 0.0..1.0 ||
        radius !in 0.002..0.1 || width !in 1..20000 || height !in 1..20000 ||
        !Regex("[0-9a-f]{64}").matches(hash)) return null
    return BallTarget(x, y, radius, width, height, hash)
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
