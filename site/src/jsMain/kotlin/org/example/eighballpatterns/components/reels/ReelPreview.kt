package org.example.eighballpatterns.components.reels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.example.eighballpatterns.model.AnswerClock
import org.example.eighballpatterns.model.EightBallReelStats
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.PlayerProfile
import org.example.eighballpatterns.model.ReelPosition
import org.example.eighballpatterns.model.ReelStage

/** Inert neighbours may load photographs, but never clips or challenge shot clocks. */
@Composable
internal fun ReelPreview(position: ReelPosition, shot: EightBallShot?, player: PlayerProfile?,
    challenge: Boolean, stats: EightBallReelStats) {
    when (position.stage) {
        ReelStage.Welcome -> EightBallReelIntro({}, false)
        ReelStage.ModeChoice -> EightBallModeChoice({}, {})
        ReelStage.NewRack -> EightBallNewRack()
        ReelStage.Player -> if (shot != null) EightBallPlayerIntro(shot, player, challenge)
        ReelStage.Shot -> if (shot != null) {
            val clock = remember { AnswerClock({0.0}) }
            EightBallReelLayoutOverlay(shot=shot, player=player,
                onBallSelected={},
                answerClock=if(challenge) clock else null, inputEnabled=false, clockRunning=false,
                onLayoutReady={}, requestError=null, requestBusy=false, onRetry={},
                onPointSelected={}, onPointConfirmed={})
            EightBallReelActions(stats,false,{}, {})
        }
        ReelStage.ChallengeResults -> PoolLoading("Loading results…")
        else -> Unit
    }
}
