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

/** Form chronological runs BEFORE prioritizing players, so another player's turn stays a boundary. */
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
            (previous.meta.group in 1..2 && shot.meta.group in 0..2 && shot.meta.group != previous.meta.group) ||
            time == null || previousTime == null || time - previousTime > 180
        if (newGroup) groups.add(mutableListOf(shot)) else groups.last().add(shot)
    }
    return groups
}

/** Prefer the selected player's shuffled runs, then all remaining runs, without repeats. */
internal fun shuffledBrowseShots(shots: List<EightBallShot>, selectedPlayer: String?, seed: Int): List<EightBallShot> {
    // A page-load seed keeps navigation/recomposition stable, with a fresh order on reload.
    val groups = browseShotGroups(shots).shuffled(Random(seed))
    if (selectedPlayer == null) return groups.flatten()
    val (preferred, remaining) = groups.partition {
        playerKey(it.first().meta.player) == playerKey(selectedPlayer)
    }
    return (preferred + remaining).flatten()
}
