package org.example.eighballpatterns.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import kotlinx.browser.window
import org.example.eighballpatterns.browser.copyEightBallShareUrl
import org.example.eighballpatterns.browser.currentEightBallReelsShotId
import org.example.eighballpatterns.browser.currentEightBallReelsPlayerIntro
import org.example.eighballpatterns.browser.currentEightBallDailyChallenge
import org.example.eighballpatterns.browser.shareEightBallShotNative
import org.example.eighballpatterns.browser.updateEightBallReelsUrl
import org.example.eighballpatterns.browser.updateEightBallChallengeUrl
import org.example.eighballpatterns.components.reels.EightBallChallengeHeader
import org.example.eighballpatterns.components.reels.EightBallChallengeResults
import org.example.eighballpatterns.components.reels.EightBallModeChoice
import org.example.eighballpatterns.components.reels.EightBallChallengeEntry
import org.example.eighballpatterns.components.reels.EightBallFullscreenImage
import org.example.eighballpatterns.components.reels.EightBallPlayerIntro
import org.example.eighballpatterns.components.reels.EightBallReelIntro
import org.example.eighballpatterns.components.reels.EightBallReelMessage
import org.example.eighballpatterns.components.reels.EightBallReelShot
import org.example.eighballpatterns.components.reels.EightBallReelsShell
import org.example.eighballpatterns.components.reels.EightBallShareSheet
import org.example.eighballpatterns.components.reels.eightBallReelLikeLabel
import org.example.eighballpatterns.components.reels.ensureEightBallReelsStyles
import org.example.eighballpatterns.data.loadEightBallShots
import org.example.eighballpatterns.data.loadPlayerProfiles
import org.example.eighballpatterns.data.currentChallengeDate
import org.example.eighballpatterns.data.FirebaseChallengeRepository
import org.example.eighballpatterns.model.AnswerClock
import org.example.eighballpatterns.model.EightBallReelStats
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.PlayerProfile
import org.example.eighballpatterns.model.ReelPosition
import org.example.eighballpatterns.model.ReelStage
import org.example.eighballpatterns.model.buildEightBallReelFeedback
import org.w3c.dom.HTMLVideoElement

private const val EIGHT_BALL_SLIDE_DURATION_MS = 320
private const val EIGHT_BALL_REEL_TOUCH_THRESHOLD = 64.0

@Page("/")
@Composable
fun ShotPredictionPage() {
    val initialShotId = remember { currentEightBallReelsShotId() }
    val initialPlayerIntro = remember { currentEightBallReelsPlayerIntro() }
    val initialChallenge = remember { currentEightBallDailyChallenge() }
    var shots by remember { mutableStateOf<List<EightBallShot>?>(null) }
    var players by remember { mutableStateOf(emptyMap<String, PlayerProfile>()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var position by remember { mutableStateOf(ReelPosition()) }
    var selectedBall by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var videoEnded by remember { mutableStateOf(false) }
    var videoPaused by remember { mutableStateOf(false) }
    var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
    var touchStartY by remember { mutableStateOf<Double?>(null) }
    var isAdvancing by remember { mutableStateOf(false) }
    var imageFullscreen by remember { mutableStateOf(false) }
    var likedShotIds by remember { mutableStateOf(emptySet<String>()) }
    var shareShot by remember { mutableStateOf<EightBallShot?>(null) }
    var shareCopied by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(EightBallReelStats()) }
    var challengeMode by remember { mutableStateOf(false) }
    val daily = remember { FirebaseChallengeRepository() }
    var challengeError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(daily) {
        val unsubscribe = daily.observePlayer()
        onDispose { unsubscribe() }
    }

    LaunchedEffect(Unit) {
        ensureEightBallReelsStyles()
        loadPlayerProfiles { players = it }
        loadEightBallShots(
            onLoaded = {
                shots = it
                val requestedIndex = it.indexOfFirst { shot -> shot.entry.id == initialShotId }
                if (initialChallenge) {
                    position = ReelPosition(ReelStage.ModeChoice)
                } else if (requestedIndex >= 0) {
                    position = ReelPosition(
                        if (initialPlayerIntro) ReelStage.Player else ReelStage.Shot,
                        requestedIndex,
                    )
                }
                loadError = null
            },
            onError = { loadError = it },
        )
    }

    val loadedShots = shots
    val today = currentChallengeDate()
    val attempt = daily.attempt?.takeIf { challengeMode || it.challenge.date == today }
    val reelShots = if (challengeMode) attempt?.challenge?.shotIds?.mapNotNull { id ->
        loadedShots?.find { it.entry.id == id }
    } else loadedShots
    val rawShot = reelShots?.getOrNull(position.shotIndex)
    val currentShot = if (challengeMode && rawShot != null) {
        val answer = daily.attempt?.answers?.find { it.shotId == rawShot.entry.id }
        rawShot.copy(meta = rawShot.meta.copy(ball = answer?.correctBall ?: -1))
    } else rawShot
    val answerClock = remember(challengeMode, currentShot?.entry?.id, attempt?.challenge?.date, daily.clockRevision) {
        if (challengeMode) AnswerClock({ window.performance.now() }, attempt?.currentShotElapsedMs ?: 0) else null
    }
    val displayedStats = if (challengeMode) attempt?.answers?.fold(EightBallReelStats()) { value, answer -> value.next(answer.correct) }
        ?: EightBallReelStats() else stats

    LaunchedEffect(position, loadedShots, challengeMode) {
        if (loadedShots == null) return@LaunchedEffect
        if (challengeMode) {
            attempt?.let { updateEightBallChallengeUrl(it.challenge.date) }
        } else {
            val shot = currentShot?.takeIf { position.stage == ReelStage.Player || position.stage == ReelStage.Shot }
            updateEightBallReelsUrl(shot?.entry?.id, position.stage == ReelStage.Player)
        }
    }

    LaunchedEffect(position.stage, daily.attempt?.complete) {
        if (position.stage == ReelStage.ChallengeResults && daily.attempt?.complete == true) {
            daily.loadLeaderboard()
        }
    }

    LaunchedEffect(position.stage, daily.player?.uid, daily.authReady) {
        if (position.stage == ReelStage.ChallengeEntry && daily.statusDate != currentChallengeDate()) {
            daily.refreshStatus()
        }
    }

    fun resetPrediction() {
        answerClock?.pause()
        videoElement?.let { video ->
            video.pause()
            video.currentTime = 0.0
        }
        selectedBall = null
        feedback = null
        videoEnded = false
        videoPaused = false
        touchStartY = null
    }

    fun moveReel(delta: Int) {
        val shotCount = reelShots?.size ?: return
        if (shotCount == 0 || isAdvancing || imageFullscreen || shareShot != null) return
        if (position.stage in listOf(ReelStage.ModeChoice, ReelStage.ChallengeEntry, ReelStage.ChallengeResults)) return
        if (challengeMode && (daily.busy || delta < 0 || (position.stage == ReelStage.Shot && selectedBall == null))) return
        isAdvancing = true
        resetPrediction()
        position = if (challengeMode && position.stage == ReelStage.Shot && position.shotIndex == shotCount - 1) {
            ReelPosition(ReelStage.ChallengeResults)
        } else if (delta > 0) position.next(shotCount) else position.previous()
        window.setTimeout({ isAdvancing = false }, EIGHT_BALL_SLIDE_DURATION_MS)
    }

    fun browse() {
        resetPrediction()
        challengeMode = false
        stats = EightBallReelStats()
        position = ReelPosition(ReelStage.Player)
        isAdvancing = false
    }

    fun startChallenge(alias: String) {
        val available = loadedShots ?: return
        challengeError = null
        daily.open(alias) { active ->
            if (position.stage != ReelStage.ChallengeEntry) return@open
            if (active.challenge.shotIds.any { id -> available.none { it.entry.id == id } }) {
                challengeError = "Today's shots aren't available in this version. Please reload the page."
            } else {
                resetPrediction()
                challengeMode = true
                position = ReelPosition(if (active.complete) ReelStage.ChallengeResults else ReelStage.Player, active.answers.size)
                isAdvancing = false
            }
        }
    }

    fun leaveChallenge() {
        resetPrediction()
        challengeMode = false
        position = ReelPosition(ReelStage.ModeChoice)
        isAdvancing = false
    }

    fun nextChallengeShot() {
        if (position.stage != ReelStage.Shot || selectedBall == null || daily.busy) return
        isAdvancing = false
        moveReel(1)
    }

    fun playVideo(video: HTMLVideoElement) {
        video.play().catch {
            if (videoElement == video && selectedBall != null && video.paused) videoPaused = true
        }
    }

    LaunchedEffect(position, selectedBall, videoElement) {
        if (selectedBall != null) {
            videoElement?.let { video ->
                video.currentTime = 0.0
                videoPaused = false
                if (video.paused) playVideo(video)
            }
        }
    }

    fun toggleVideoPlayback() {
        val video = videoElement ?: return
        if (videoEnded) {
            video.currentTime = 0.0
            videoEnded = false
            videoPaused = false
            playVideo(video)
        } else if (video.paused) {
            videoPaused = false
            playVideo(video)
        } else {
            videoPaused = true
            video.pause()
        }
    }

    LaunchedEffect(challengeMode, position, daily.attempt?.answers) {
        if (challengeMode && position.stage == ReelStage.Shot) {
            daily.attempt?.answers?.find { it.shotId == currentShot?.entry?.id }?.let { answer ->
                if (selectedBall == null) {
                    selectedBall = answer.selectedBall
                    feedback = buildEightBallReelFeedback(answer.correct)
                }
            }
        }
    }

    EightBallReelsShell(
        onNext = { moveReel(1) },
        onPrevious = { moveReel(-1) },
        allowContentScroll = position.stage in listOf(ReelStage.ModeChoice, ReelStage.ChallengeEntry, ReelStage.ChallengeResults),
        onTouchStart = { y -> touchStartY = y },
        onTouchCancel = { touchStartY = null },
        onTouchEnd = { y ->
            val startY = touchStartY
            touchStartY = null
            if (startY != null) {
                val deltaY = y - startY
                when {
                    deltaY <= -EIGHT_BALL_REEL_TOUCH_THRESHOLD -> moveReel(1)
                    deltaY >= EIGHT_BALL_REEL_TOUCH_THRESHOLD -> moveReel(-1)
                }
            }
        },
    ) {
        key(challengeMode, position.stage, currentShot?.entry?.id) {
            when {
                loadError != null -> EightBallReelMessage(loadError ?: "Could not load shots.")
                loadedShots?.isEmpty() == true -> EightBallReelMessage("No shots.")
                position.stage == ReelStage.Welcome && (initialShotId == null || loadedShots != null) -> EightBallReelIntro(
                    onStart = { moveReel(1) },
                    canStart = loadedShots != null,
                )
                loadedShots == null -> EightBallReelMessage("Loading...")
                position.stage == ReelStage.ModeChoice -> EightBallModeChoice(
                    onDailyChallenge = { position = ReelPosition(ReelStage.ChallengeEntry) },
                    onBrowse = { browse() },
                )
                position.stage == ReelStage.ChallengeEntry -> EightBallChallengeEntry(
                    attempt = attempt,
                    date = daily.statusDate,
                    shotCount = daily.shotCount,
                    onDailyChallenge = { startChallenge(it) },
                    onBack = { position = ReelPosition(ReelStage.ModeChoice) },
                    alias = daily.alias,
                    aliasLocked = daily.attempt?.challenge?.date == today,
                    busy = daily.busy || !daily.authReady,
                    error = challengeError ?: daily.error,
                    onRetry = { daily.refreshStatus() },
                )
                position.stage == ReelStage.ChallengeResults && attempt?.complete == true -> EightBallChallengeResults(
                    attempt = attempt,
                    onBrowse = { browse() },
                    onBack = { leaveChallenge() },
                    leaderboard = daily.leaderboard,
                    playerUid = daily.player?.uid,
                    leaderboardBusy = daily.leaderboardBusy,
                    leaderboardError = daily.leaderboardError,
                    onRefresh = { daily.loadLeaderboard() },
                    onLoadMore = { daily.loadLeaderboard(more = true) },
                )
                position.stage == ReelStage.Player && currentShot != null -> EightBallPlayerIntro(
                    shot = currentShot,
                    player = players[currentShot.meta.player.trim().lowercase()],
                    challenge = challengeMode,
                )
                currentShot != null -> {
                    EightBallReelShot(
                        shot = currentShot,
                        selectedBall = selectedBall,
                        feedback = feedback,
                        videoEnded = videoEnded,
                        videoPaused = videoPaused,
                        stats = displayedStats,
                        liked = currentShot.entry.id in likedShotIds,
                        likesLabel = eightBallReelLikeLabel(currentShot.entry.id),
                        onImageFullscreen = { imageFullscreen = true },
                        onLike = {
                            likedShotIds = if (currentShot.entry.id in likedShotIds) {
                                likedShotIds - currentShot.entry.id
                            } else {
                                likedShotIds + currentShot.entry.id
                            }
                        },
                        onShare = {
                            if (!shareEightBallShotNative(currentShot)) {
                                shareShot = currentShot
                                shareCopied = false
                            }
                        },
                        onChooseMode = if (challengeMode) null else ({
                            resetPrediction()
                            position = ReelPosition(ReelStage.ModeChoice)
                        }),
                        onBallSelected = { ball ->
                            if (selectedBall != null) return@EightBallReelShot
                            if (challengeMode) {
                                val active = daily.attempt ?: return@EightBallReelShot
                                if (active.currentShotId != currentShot.entry.id) return@EightBallReelShot
                                daily.answer(currentShot.entry.id, ball) {
                                    val answer = daily.attempt?.answers?.find { it.shotId == currentShot.entry.id }
                                    if (answer != null) {
                                        answerClock?.pause()
                                        selectedBall = answer.selectedBall
                                        feedback = buildEightBallReelFeedback(answer.correct)
                                        videoEnded = false
                                        videoPaused = false
                                    }
                                }
                                return@EightBallReelShot
                            }
                            val correct = ball == currentShot.meta.ball
                            selectedBall = ball
                            videoEnded = false
                            videoPaused = false
                            feedback = buildEightBallReelFeedback(correct)
                            stats = stats.next(correct)
                            videoElement?.let { video ->
                                video.currentTime = 0.0
                                playVideo(video)
                            }
                        },
                        onVideoElement = { videoElement = it },
                        onVideoEnded = {
                            videoEnded = true
                            videoPaused = false
                        },
                        onVideoPlay = {
                            videoEnded = false
                            videoPaused = false
                        },
                        onVideoPause = {
                            if (!videoEnded) videoPaused = true
                        },
                        onVideoToggle = { toggleVideoPlayback() },
                        answerClock = answerClock,
                        inputEnabled = !challengeMode || (daily.clockStarted && !daily.busy),
                        clockRunning = challengeMode && daily.clockStarted,
                        onLayoutReady = { if (challengeMode) daily.startShot(currentShot.entry.id) },
                        requestError = if (challengeMode) daily.error else null,
                        requestBusy = challengeMode && daily.busy,
                        onRetry = { daily.retryRequest() },
                        onChallengeNext = if (challengeMode) ({ nextChallengeShot() }) else null,
                        challengeLastShot = challengeMode && position.shotIndex == reelShots?.lastIndex,
                    )

                    if (imageFullscreen) {
                        EightBallFullscreenImage(
                            shot = currentShot,
                            onClose = { imageFullscreen = false }
                        )
                    }
                }
            }
        }

        if (challengeMode && (position.stage == ReelStage.Player || position.stage == ReelStage.Shot) && currentShot != null && !imageFullscreen && shareShot == null) {
            EightBallChallengeHeader(position.shotIndex + 1, reelShots.orEmpty().size, onLeave = { leaveChallenge() })
        }

        shareShot?.let { shot ->
            EightBallShareSheet(
                shot = shot,
                copied = shareCopied,
                onCopy = {
                    copyEightBallShareUrl(shot)
                    shareCopied = true
                },
                onClose = {
                    shareShot = null
                    shareCopied = false
                },
            )
        }
    }
}
