package org.example.eighballpatterns.model

internal data class LeaderboardEntry(
    val uid: String,
    val displayName: String,
    val correctCount: Int,
    val shotCount: Int,
    val totalElapsedMs: Int,
    val rank: Int,
    val simulated: Boolean = false,
)

internal data class DailyLeaderboard(
    val entries: List<LeaderboardEntry> = emptyList(),
    val total: Int = 0,
    val simulatedCount: Int = 0,
    val ownRank: Int? = null,
    val nextAfterUid: String? = null,
)
