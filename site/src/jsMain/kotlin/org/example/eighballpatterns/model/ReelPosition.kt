package org.example.eighballpatterns.model

internal enum class ReelStage { Welcome, ModeChoice, Player, Shot, ChallengeResults }

internal data class ReelPosition(
    val stage: ReelStage = ReelStage.Welcome,
    val shotIndex: Int = 0,
) {
    fun next(shotCount: Int): ReelPosition = when (stage) {
        ReelStage.Welcome -> ReelPosition(ReelStage.ModeChoice)
        ReelStage.ModeChoice, ReelStage.ChallengeResults -> this
        ReelStage.Player -> copy(stage = ReelStage.Shot)
        ReelStage.Shot -> ReelPosition(ReelStage.Player, (shotIndex + 1) % shotCount)
    }

    fun previous(): ReelPosition = when (stage) {
        ReelStage.Welcome -> this
        ReelStage.ModeChoice -> ReelPosition()
        ReelStage.ChallengeResults -> ReelPosition(ReelStage.ModeChoice)
        ReelStage.Player -> if (shotIndex == 0) ReelPosition(ReelStage.ModeChoice) else ReelPosition(ReelStage.Shot, shotIndex - 1)
        ReelStage.Shot -> copy(stage = ReelStage.Player)
    }
}
