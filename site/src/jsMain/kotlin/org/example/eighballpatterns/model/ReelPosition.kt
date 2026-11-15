package org.example.eighballpatterns.model

internal enum class ReelStage { Welcome, PlayerChoice, Player, Shot, ChallengeResults }

internal data class ReelPosition(
    val stage: ReelStage = ReelStage.Welcome,
    val shotIndex: Int = 0,
) {
    fun next(shotCount: Int, nextStartsGroup: Boolean = true): ReelPosition = when (stage) {
        ReelStage.Welcome -> copy(stage = ReelStage.Player)
        ReelStage.ChallengeResults -> ReelPosition(ReelStage.Player)
        ReelStage.PlayerChoice -> this
        ReelStage.Player -> copy(stage = ReelStage.Shot)
        ReelStage.Shot -> ReelPosition(if (nextStartsGroup) ReelStage.Player else ReelStage.Shot, (shotIndex + 1) % shotCount)
    }

    fun previous(currentStartsGroup: Boolean = true, wrapBrowse: Boolean = false, shotCount: Int = shotIndex + 1): ReelPosition = when (stage) {
        ReelStage.Welcome -> this
        ReelStage.PlayerChoice, ReelStage.ChallengeResults -> this
        ReelStage.Player -> if (shotIndex == 0 && !wrapBrowse) this
            else ReelPosition(ReelStage.Shot, (shotIndex - 1 + shotCount) % shotCount)
        ReelStage.Shot -> if (currentStartsGroup) copy(stage = ReelStage.Player)
            else ReelPosition(ReelStage.Shot, (shotIndex - 1).coerceAtLeast(0))
    }
}
