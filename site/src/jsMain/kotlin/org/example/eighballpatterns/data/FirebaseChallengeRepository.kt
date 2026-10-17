package org.example.eighballpatterns.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.eighballpatterns.model.ChallengeAnswer
import org.example.eighballpatterns.model.DailyChallenge
import org.example.eighballpatterns.model.DailyChallengeAttempt
import org.example.eighballpatterns.model.DailyLeaderboard
import org.example.eighballpatterns.model.LeaderboardEntry

internal class FirebaseChallengeRepository {
    var player by mutableStateOf<ChallengePlayer?>(null)
        private set
    var authReady by mutableStateOf(false)
        private set
    var alias by mutableStateOf<String?>(null)
        private set
    var statusDate by mutableStateOf<String?>(null)
        private set
    var attempt by mutableStateOf<DailyChallengeAttempt?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var clockStarted by mutableStateOf(false)
        private set
    var clockRevision by mutableStateOf(0)
        private set
    var leaderboard by mutableStateOf<DailyLeaderboard?>(null)
        private set
    var leaderboardBusy by mutableStateOf(false)
        private set
    var leaderboardError by mutableStateOf<String?>(null)
        private set
    private var retry: (() -> Unit)? = null
    private var identityRevision = 0

    fun retryRequest() { retry?.invoke() }

    fun observePlayer(): () -> Unit = FirebaseClient.observePlayer { next ->
        if (next?.uid != player?.uid) {
            identityRevision++
            alias = null
            statusDate = null
            attempt = null
            leaderboard = null
            error = null
            retry = null
            clockStarted = false
        }
        player = next
        authReady = true
        if (next != null && !busy) refreshStatus()
    }

    fun refreshStatus() {
        if (player == null || busy) return
        val revision = identityRevision
        busy = true
        error = null
        FirebaseClient.call("getDailyChallengeStatus").then<Unit> { data: dynamic ->
            if (revision != identityRevision) return@then
            alias = data.alias as? String
            statusDate = data.date as String
            if (data.attempt != null) updateAttempt(data.attempt) else attempt = null
            busy = false
        }.catch { failure ->
            if (revision == identityRevision) {
                busy = false
                error = firebaseErrorMessage(failure)
            }
        }
    }

    fun open(chosenAlias: String, onReady: (DailyChallengeAttempt) -> Unit) {
        if (busy || !authReady) return
        busy = true
        error = null
        if (player == null) {
            FirebaseClient.signIn().then<Unit> { result: dynamic ->
                val uid = result.user.uid as String
                if (player?.uid != uid) {
                    identityRevision++
                    player = ChallengePlayer(uid)
                }
                loadAttempt(chosenAlias, onReady)
            }.catch { failure ->
                busy = false
                error = firebaseErrorMessage(failure)
            }
        } else loadAttempt(chosenAlias, onReady)
    }

    private fun loadAttempt(chosenAlias: String, onReady: (DailyChallengeAttempt) -> Unit) {
        val revision = identityRevision
        val input = js("({})")
        input.alias = chosenAlias
        FirebaseClient.call("startDailyChallenge", input).then<Unit> { data: dynamic ->
            if (revision != identityRevision) return@then
            updateAttempt(data)
            busy = false
            onReady(attempt!!)
        }.catch { failure ->
            busy = false
            error = firebaseErrorMessage(failure)
        }
    }

    fun startShot(shotId: String) {
        val active = attempt ?: return
        if (busy || active.currentShotId != shotId) return
        requestAttempt("startChallengeShot", shotId)
    }

    fun answer(shotId: String, selectedBall: Int, onAccepted: () -> Unit) {
        if (busy || !clockStarted) return
        requestAttempt("submitChallengeAnswer", shotId, selectedBall, onAccepted)
    }

    private fun requestAttempt(name: String, shotId: String, selectedBall: Int? = null, onReady: () -> Unit = {}) {
        val active = attempt ?: return
        val input = js("({})")
        input.date = active.challenge.date
        input.shotId = shotId
        if (selectedBall != null) input.selectedBall = selectedBall
        busy = true
        error = null
        retry = null
        FirebaseClient.call(name, input).then<Unit> { data: dynamic ->
            updateAttempt(data, shotReady = name == "startChallengeShot")
            busy = false
            onReady()
        }.catch { failure ->
            busy = false
            error = firebaseErrorMessage(failure)
            retry = { requestAttempt(name, shotId, selectedBall, onReady) }
        }
    }

    private fun updateAttempt(data: dynamic, shotReady: Boolean = false) {
        alias = data.displayName as String
        statusDate = data.date as String
        attempt = DailyChallengeAttempt(
            DailyChallenge(data.date as String, (data.shotIds as Array<String>).toList()),
            (data.answers as Array<dynamic>).map { answer ->
                ChallengeAnswer(answer.shotId as String, answer.selectedBall as Int, answer.correctBall as Int, answer.elapsedMs as Int)
            },
            data.currentShotElapsedMs as Int,
        )
        // Resume data alone must not enable picking before this layout's start request finishes.
        clockStarted = shotReady && data.clockStarted == true
        clockRevision++
    }

    fun loadLeaderboard(more: Boolean = false) {
        val active = attempt ?: return
        if (!active.complete || leaderboardBusy) return
        val input = js("({})")
        input.date = active.challenge.date
        if (more) input.afterUid = leaderboard?.nextAfterUid ?: return
        leaderboardBusy = true
        leaderboardError = null
        FirebaseClient.call("getDailyLeaderboard", input).then<Unit> { data: dynamic ->
            val entries = (data.entries as Array<dynamic>).map { entry ->
                LeaderboardEntry(entry.uid as String, entry.displayName as String, entry.correctCount as Int,
                    entry.shotCount as Int, entry.totalElapsedMs as Int, entry.rank as Int,
                    entry.simulated == true)
            }
            leaderboard = DailyLeaderboard(
                entries = ((if (more) leaderboard?.entries.orEmpty() else emptyList()) + entries).distinctBy { it.uid },
                total = data.total as Int,
                simulatedCount = data.simulatedCount as Int,
                ownRank = data.ownRank as? Int,
                nextAfterUid = data.nextAfterUid as? String,
            )
            leaderboardBusy = false
        }.catch { failure ->
            leaderboardBusy = false
            leaderboardError = firebaseErrorMessage(failure)
        }
    }
}
