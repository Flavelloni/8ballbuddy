package org.example.eighballpatterns.model

internal enum class ReelStage { Welcome, ModeChoice, PlayerChoice, ChallengeEntry, NewRack, Player, Shot, ChallengeResults }

internal data class ReelPosition(
    val stage: ReelStage = ReelStage.Welcome,
    val shotIndex: Int = 0,
) {
    fun next(shotCount: Int, nextStartsGroup: Boolean = true, announceRack: Boolean = false): ReelPosition = when (stage) {
        ReelStage.Welcome -> ReelPosition(ReelStage.ModeChoice)
        ReelStage.ModeChoice, ReelStage.PlayerChoice, ReelStage.ChallengeEntry, ReelStage.ChallengeResults -> this
        ReelStage.Player -> copy(stage = ReelStage.Shot)
        ReelStage.NewRack -> copy(stage = ReelStage.Player)
        ReelStage.Shot -> ReelPosition(if (!nextStartsGroup) ReelStage.Shot else if (announceRack) ReelStage.NewRack else ReelStage.Player, (shotIndex + 1) % shotCount)
    }

    fun previous(currentStartsGroup: Boolean = true, announceRack: Boolean = false, shotCount: Int = shotIndex + 1): ReelPosition = when (stage) {
        ReelStage.Welcome -> this
        ReelStage.ModeChoice -> ReelPosition()
        ReelStage.PlayerChoice -> ReelPosition(ReelStage.ModeChoice)
        ReelStage.ChallengeEntry -> ReelPosition(ReelStage.ModeChoice)
        ReelStage.ChallengeResults -> ReelPosition(ReelStage.ModeChoice)
        ReelStage.NewRack -> ReelPosition(ReelStage.Shot, (shotIndex - 1 + shotCount) % shotCount)
        ReelStage.Player -> if (announceRack) copy(stage = ReelStage.NewRack)
            else if (shotIndex == 0) ReelPosition(ReelStage.ModeChoice) else ReelPosition(ReelStage.Shot, shotIndex - 1)
        ReelStage.Shot -> if (currentStartsGroup) copy(stage = ReelStage.Player)
            else ReelPosition(ReelStage.Shot, (shotIndex - 1).coerceAtLeast(0))
    }
}
