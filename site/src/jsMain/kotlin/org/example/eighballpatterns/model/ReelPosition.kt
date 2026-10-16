package org.example.eighballpatterns.model

internal enum class ReelStage { Welcome, Player, Shot }

internal data class ReelPosition(
    val stage: ReelStage = ReelStage.Welcome,
    val shotIndex: Int = 0,
) {
    fun next(shotCount: Int): ReelPosition = when (stage) {
        ReelStage.Welcome -> ReelPosition(ReelStage.Player)
        ReelStage.Player -> copy(stage = ReelStage.Shot)
        ReelStage.Shot -> ReelPosition(ReelStage.Player, (shotIndex + 1) % shotCount)
    }

    fun previous(): ReelPosition = when (stage) {
        ReelStage.Welcome -> this
        ReelStage.Player -> if (shotIndex == 0) ReelPosition() else ReelPosition(ReelStage.Shot, shotIndex - 1)
        ReelStage.Shot -> copy(stage = ReelStage.Player)
    }
}
