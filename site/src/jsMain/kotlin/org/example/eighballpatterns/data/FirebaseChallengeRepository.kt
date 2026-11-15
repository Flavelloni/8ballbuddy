package org.example.eighballpatterns.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.eighballpatterns.model.ChallengeAnswer
import org.example.eighballpatterns.model.DailyChallenge
import org.example.eighballpatterns.model.DailyChallengeAttempt
import org.example.eighballpatterns.model.DailyLeaderboard
import org.example.eighballpatterns.model.LeaderboardEntry
import org.example.eighballpatterns.model.withPlayerName
import org.example.eighballpatterns.browser.markAutomaticChallengeName

internal class FirebaseChallengeRepository {
    var player by mutableStateOf<ChallengePlayer?>(null)
        private set
    var authReady by mutableStateOf(false)
        private set
    var alias by mutableStateOf<String?>(null)
        private set
    var statusDate by mutableStateOf<String?>(null)
        private set
    var shotCount by mutableStateOf<Int?>(null)
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
    var allTimeLeaderboard by mutableStateOf<DailyLeaderboard?>(null)
        private set
    var allTimeBusy by mutableStateOf(false)
        private set
    var allTimeError by mutableStateOf<String?>(null)
        private set
    var renameBusy by mutableStateOf(false)
        private set
    var renameError by mutableStateOf<String?>(null)
        private set
    private var retry: (() -> Unit)? = null
    private var identityRevision = 0

    fun retryRequest() { retry?.invoke() }

    fun observePlayer(): () -> Unit = FirebaseClient.observePlayer { next ->
        if (next?.uid != player?.uid) {
            identityRevision++
            alias = null
            statusDate = null
            shotCount = null
            attempt = null
            leaderboard = null
            allTimeLeaderboard = null
            allTimeError = null
            renameError = null
            error = null
            retry = null
            clockStarted = false
        }
        player = next
        authReady = true
        if (!busy) refreshStatus()
    }

    fun refreshStatus() {
        if (busy || !authReady) return
        val revision = identityRevision
        busy = true
        error = null
        FirebaseClient.call("getDailyChallengeStatus").then<Unit> { data: dynamic ->
            if (revision != identityRevision) return@then
            alias = data.alias as? String
            statusDate = data.date as String
            shotCount = data.shotCount as Int
            if (data.attempt != null) updateAttempt(data.attempt) else attempt = null
            busy = false
        }.catch { failure ->
            if (revision == identityRevision) {
                busy = false
                error = firebaseErrorMessage(failure)
            }
        }
    }

    fun open(onReady: (DailyChallengeAttempt) -> Unit) {
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
                loadAttempt(onReady)
            }.catch { failure ->
                busy = false
                error = firebaseErrorMessage(failure)
            }
        } else loadAttempt(onReady)
    }

    private fun loadAttempt(onReady: (DailyChallengeAttempt) -> Unit) {
        val revision = identityRevision
        val assigningName = alias == null
        val input = js("({})")
        FirebaseClient.call("startDailyChallenge", input).then<Unit> { data: dynamic ->
            if (revision != identityRevision) return@then
            updateAttempt(data)
            if (assigningName) player?.uid?.let { markAutomaticChallengeName(it) }
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

    fun answerBall(shotId: String, selectedBallId: String, onAccepted: () -> Unit) {
        if (busy || !clockStarted) return
        requestAttempt("submitChallengeAnswer",shotId,onReady=onAccepted,selectedBallId=selectedBallId)
    }

    private fun requestAttempt(name: String, shotId: String, selectedBall: Int? = null, onReady: () -> Unit = {}, selectedBallId: String? = null) {
        val active = attempt ?: return
        val input = js("({})")
        input.date = active.challenge.date
        input.shotId = shotId
        if (selectedBall != null) input.selectedBall = selectedBall
        if (selectedBallId != null) input.selectedBallId = selectedBallId
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
            retry = { requestAttempt(name, shotId, selectedBall, onReady, selectedBallId) }
        }
    }

    private fun updateAttempt(data: dynamic, shotReady: Boolean = false) {
        alias = data.displayName as String
        statusDate = data.date as String
        shotCount = (data.shotIds as Array<String>).size
        attempt = DailyChallengeAttempt(
            DailyChallenge(data.date as String, (data.shotIds as Array<String>).toList()),
            (data.answers as Array<dynamic>).map { answer ->
                ChallengeAnswer(answer.shotId as String, answer.selectedBall as Int, answer.correctBall as Int, answer.elapsedMs as Int, answer.correct as? Boolean)
            },
            data.currentShotElapsedMs as Int,
        )
        // Resume data alone must not enable picking before this layout's start request finishes.
        clockStarted = shotReady && data.clockStarted == true
        clockRevision++
    }

    fun loadLeaderboard(more: Boolean = false, allTime: Boolean = false) {
        val active = attempt ?: return
        if (!active.complete || (if (allTime) allTimeBusy else leaderboardBusy)) return
        val revision = identityRevision
        val requestedAlias = alias
        val source = if (allTime) allTimeLeaderboard else leaderboard
        val input = js("({})")
        input.date = active.challenge.date
        if (more) input.afterUid = source?.nextAfterUid ?: return
        if (allTime) { allTimeBusy = true; allTimeError = null }
        else { leaderboardBusy = true; leaderboardError = null }
        FirebaseClient.call(if (allTime) "getAllTimeLeaderboard" else "getDailyLeaderboard", input).then<Unit> { data: dynamic ->
            if (revision != identityRevision) return@then
            fun parse(entry: dynamic): LeaderboardEntry =
                LeaderboardEntry(entry.uid as String,
                    if (entry.uid == player?.uid && alias != requestedAlias) alias ?: (entry.displayName as String) else entry.displayName as String,
                    entry.correctCount as Int,
                    entry.shotCount as Int, entry.totalElapsedMs as Int, entry.rank as Int,
                    entry.simulated == true)
            val entries = (data.entries as Array<dynamic>).map { parse(it) }
            val board = DailyLeaderboard(
                entries = ((if (more) source?.entries.orEmpty() else emptyList()) + entries).distinctBy { it.uid },
                total = data.total as Int,
                simulatedCount = data.simulatedCount as Int,
                ownRank = data.ownRank as? Int,
                nextAfterUid = data.nextAfterUid as? String,
                ownEntry = if (data.ownEntry != null) parse(data.ownEntry) else null,
            )
            if (allTime) { allTimeLeaderboard = board; allTimeBusy = false }
            else { leaderboard = board; leaderboardBusy = false }
        }.catch { failure ->
            if (revision == identityRevision) {
                if (allTime) { allTimeBusy = false; allTimeError = firebaseErrorMessage(failure, "all-time leaderboard") }
                else { leaderboardBusy = false; leaderboardError = firebaseErrorMessage(failure, "daily leaderboard") }
            }
        }
    }

    fun rename(chosenAlias: String, onSaved: () -> Unit) {
        if (renameBusy || player == null) return
        val revision = identityRevision
        renameBusy = true
        renameError = null
        val input = js("({})")
        input.alias = chosenAlias
        FirebaseClient.call("renamePlayer", input).then<Unit> { data: dynamic ->
            if (revision != identityRevision) return@then
            alias = data.alias as String
            player?.uid?.let { uid ->
                leaderboard = leaderboard?.withPlayerName(uid, alias!!)
                allTimeLeaderboard = allTimeLeaderboard?.withPlayerName(uid, alias!!)
            }
            renameBusy = false
            loadLeaderboard()
            loadLeaderboard(allTime = true)
            onSaved()
        }.catch { failure ->
            if (revision == identityRevision) {
                renameBusy = false
                renameError = firebaseErrorMessage(failure)
            }
        }
    }
}
