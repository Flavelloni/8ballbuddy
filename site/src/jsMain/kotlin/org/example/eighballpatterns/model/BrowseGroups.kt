package org.example.eighballpatterns.model

import kotlin.random.Random

internal fun playerKey(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase()

internal fun availableBrowsePlayers(shots: List<EightBallShot>): List<String> =
    shots.filterNot { it.meta.archived }.map { it.meta.player.trim() }.filter { it.isNotBlank() }
        .distinctBy(::playerKey).sortedBy(::playerKey)

private fun EightBallShot.matchPath(): String =
    entry.metaPath.substringBefore('?').substringBeforeLast('/').substringBeforeLast('/')

private fun EightBallShot.broadcastSeconds(): Int? {
    val time = entry.id.substringAfterLast('/').substringBefore('_')
    if (!Regex("[0-9]{6}").matches(time)) return null
    val hours = time.take(2).toInt()
    val minutes = time.substring(2, 4).toInt()
    val seconds = time.takeLast(2).toInt()
    return if (minutes < 60 && seconds < 60) hours * 3600 + minutes * 60 + seconds else null
}

/** Form chronological runs BEFORE filtering players, so another player's turn stays a boundary. */
internal fun browseShotGroups(shots: List<EightBallShot>): List<List<EightBallShot>> {
    val ordered = shots.filterNot { it.meta.archived }.sortedWith(
        compareBy<EightBallShot> { it.matchPath() }
            .thenBy { it.broadcastSeconds() ?: Int.MAX_VALUE }.thenBy { it.entry.id },
    )
    val groups = mutableListOf<MutableList<EightBallShot>>()
    for (shot in ordered) {
        val previous = groups.lastOrNull()?.lastOrNull()
        val time = shot.broadcastSeconds()
        val previousTime = previous?.broadcastSeconds()
        val newGroup = previous == null || shot.meta.isBreak ||
            shot.matchPath() != previous.matchPath() ||
            shot.entry.id.substringBefore('/') != previous.entry.id.substringBefore('/') ||
            playerKey(shot.meta.player) != playerKey(previous.meta.player) ||
            time == null || previousTime == null || time - previousTime > 180
        if (newGroup) groups.add(mutableListOf(shot)) else groups.last().add(shot)
    }
    return groups
}

/** A page-load seed makes the shuffle stable during rendering/navigation, fresh after reload. */
internal fun shuffledBrowseShots(shots: List<EightBallShot>, selectedPlayer: String?, seed: Int): List<EightBallShot> =
    browseShotGroups(shots)
        .filter { selectedPlayer == null || playerKey(it.first().meta.player) == playerKey(selectedPlayer) }
        .shuffled(Random(seed)).flatten()
