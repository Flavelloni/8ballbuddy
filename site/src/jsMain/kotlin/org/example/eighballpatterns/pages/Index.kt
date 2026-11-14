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
import com.varabyte.kobweb.navigation.BasePath
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
import org.example.eighballpatterns.components.reels.EightBallPlayerIntro
import org.example.eighballpatterns.components.reels.EightBallPlayerChoice
import org.example.eighballpatterns.components.reels.EightBallReelIntro
import org.example.eighballpatterns.components.reels.PoolLoading
import org.example.eighballpatterns.components.reels.preloadLayoutImage
import org.example.eighballpatterns.components.reels.EightBallReelMessage
import org.example.eighballpatterns.components.reels.EightBallReelShot
import org.example.eighballpatterns.components.reels.EightBallReelsShell
import org.example.eighballpatterns.components.reels.EightBallShareSheet
import org.example.eighballpatterns.components.reels.ensureEightBallReelsStyles
import org.example.eighballpatterns.components.reels.PredictionFeedback
import org.example.eighballpatterns.components.reels.ReelPager
import org.example.eighballpatterns.components.reels.ReelPreview
import org.example.eighballpatterns.browser.primePredictionAudio
import org.example.eighballpatterns.browser.playPredictionFeedback
import org.example.eighballpatterns.data.loadEightBallShots
import org.example.eighballpatterns.data.loadPlayerProfiles
import org.example.eighballpatterns.data.currentChallengeDate
import org.example.eighballpatterns.data.FirebaseChallengeRepository
import org.example.eighballpatterns.model.AnswerClock
import org.example.eighballpatterns.model.EightBallReelStats
import org.example.eighballpatterns.model.EightBallShot
import org.example.eighballpatterns.model.PlayerProfile
import org.example.eighballpatterns.model.ImagePoint
import org.example.eighballpatterns.data.parseBallTarget
import org.example.eighballpatterns.data.parseBallSelection
import org.example.eighballpatterns.model.ReelPosition
import org.example.eighballpatterns.model.ReelStage
import org.example.eighballpatterns.model.buildEightBallReelFeedback
import org.example.eighballpatterns.model.shuffledBrowseShots
import org.example.eighballpatterns.model.availableBrowsePlayers
import org.example.eighballpatterns.model.browseShotGroups
import kotlin.random.Random
import org.w3c.dom.HTMLVideoElement

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
    var pointSelection by remember { mutableStateOf<ImagePoint?>(null) }
    var coordinateResult by remember { mutableStateOf<Boolean?>(null) }
    var resultFlash by remember { mutableStateOf<Boolean?>(null) }
    var revealTimer by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(Unit) { onDispose { revealTimer?.let { window.clearTimeout(it) } } }
    // Local annotation-tool preview links carry a draft target; never affect daily challenges.
    val previewTarget = remember {
        val params = js("new URLSearchParams(window.location.search)")
        val raw = params.get("annotationTarget") as? String
        try { if (raw != null) parseBallTarget(js("JSON.parse(raw)")) else null } catch (_: Throwable) { null }
    }
    val previewBalls = remember {
        val raw=js("new URLSearchParams(window.location.search).get('annotationBalls')") as? String
        try { if (raw!=null) parseBallSelection(js("JSON.parse(raw)")) else null } catch (_: Throwable) { null }
    }
    var feedback by remember { mutableStateOf<String?>(null) }
    var videoEnded by remember { mutableStateOf(false) }
    var videoPaused by remember { mutableStateOf(false) }
    var pulseShotId by remember { mutableStateOf<String?>(null) }
    var videoElement by remember { mutableStateOf<HTMLVideoElement?>(null) }
    val pager = remember { ReelPager() }
    var likedShotIds by remember { mutableStateOf(emptySet<String>()) }
    var shareShot by remember { mutableStateOf<EightBallShot?>(null) }
    var shareCopied by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(EightBallReelStats()) }
    var challengeMode by remember { mutableStateOf(false) }
    val browseSeed = remember { Random.nextInt() }
    var selectedPlayer by remember { mutableStateOf<String?>(null) }
    var advancedBrowseGroup by remember { mutableStateOf(false) }
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
                shots = it.map { shot ->
                    if (previewBalls != null && shot.entry.id == initialShotId && !shot.meta.isBreak)
                        shot.copy(meta=shot.meta.copy(ballSelection=previewBalls,ball=previewBalls.correctNumber))
                    else if (previewTarget != null && shot.entry.id == initialShotId && !shot.meta.isBreak)
                        shot.copy(meta=shot.meta.copy(target=previewTarget)) else shot
                }
                val requestedIndex = shuffledBrowseShots(it, null, browseSeed).indexOfFirst { shot -> shot.entry.id == initialShotId }
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
    val browseShots = remember(loadedShots, selectedPlayer, browseSeed) {
        loadedShots?.let { shuffledBrowseShots(it, selectedPlayer, browseSeed) }
    }
    val availablePlayers = remember(loadedShots) { availableBrowsePlayers(loadedShots.orEmpty()) }
    // Preserve actual run boundaries after shuffling; adjacent runs may have the same player.
    val browseGroupStarts = remember(loadedShots) {
        browseShotGroups(loadedShots.orEmpty()).map { it.first().entry.id }.toSet()
    }
    val today = currentChallengeDate()
    val attempt = daily.attempt?.takeIf { challengeMode || it.challenge.date == today }
    val reelShots = if (challengeMode) attempt?.challenge?.shotIds?.mapNotNull { id ->
        loadedShots?.find { it.entry.id == id }
    } else browseShots
    val rawShot = reelShots?.getOrNull(position.shotIndex)
    val currentShot = if (challengeMode && rawShot != null) {
        val answer = daily.attempt?.answers?.find { it.shotId == rawShot.entry.id }
        rawShot.copy(meta = rawShot.meta.copy(
            ball = if (rawShot.meta.isBreak) -1 else answer?.correctBall ?: -1,
            ballsPotted = if (rawShot.meta.isBreak) answer?.correctBall else rawShot.meta.ballsPotted,
        ))
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
        revealTimer?.let { window.clearTimeout(it) }
        revealTimer=null
        resultFlash=null
        answerClock?.pause()
        videoElement?.let { video ->
            // Keep the outgoing frame until the new reel replaces it; rewind only on play/replay.
            video.pause()
        }
        selectedBall = null
        pointSelection = null
        coordinateResult = null
        feedback = null
        videoEnded = false
        videoPaused = false
        pulseShotId = null
    }

    fun startsGroup(index: Int): Boolean = challengeMode || index == 0 ||
        reelShots?.getOrNull(index)?.entry?.id in browseGroupStarts

    fun destination(delta: Int): ReelPosition {
        val shotCount = reelShots.orEmpty().size
        if (shotCount == 0) return position
        return if (challengeMode && position.stage == ReelStage.Shot && position.shotIndex == shotCount - 1) {
            ReelPosition(ReelStage.ChallengeResults)
        } else if (delta > 0) position.next(shotCount, startsGroup((position.shotIndex + 1) % shotCount))
        else position.previous(startsGroup(position.shotIndex),
            wrapBrowse = !challengeMode && advancedBrowseGroup, shotCount = shotCount)
    }

    fun moveReel(delta: Int, animate: Boolean = true, automatic: Boolean = false) {
        val shotCount = reelShots?.size ?: return
        if (shotCount == 0 || shareShot != null || resultFlash != null) return
        if (position.stage in listOf(ReelStage.ModeChoice, ReelStage.PlayerChoice, ReelStage.ChallengeEntry, ReelStage.ChallengeResults)) return
        if (challengeMode && (daily.busy || delta < 0 || (position.stage == ReelStage.Shot && selectedBall == null))) return
        if (animate) {
            val sourcePosition = position
            val manualInputRevision = pager.manualInputRevision
            val target = destination(delta)
            val nextLayout = reelShots?.getOrNull(target.shotIndex)?.takeIf { target.stage == ReelStage.Shot }
            if (automatic && nextLayout != null) {
                // Keep the outgoing final frame while a cold/slow image loads and decodes.
                // The pager still arbitrates manual input; stale completions cannot move a new reel.
                preloadLayoutImage(nextLayout.entry.imagePath).then {
                    if (position == sourcePosition && pager.manualInputRevision == manualInputRevision &&
                        selectedBall != null && !challengeMode &&
                        shareShot == null && resultFlash == null) pager.navigate(delta, automatically = true)
                }
            } else pager.navigate(delta, automatically = automatic)
            return
        }
        val target = destination(delta)
        if (target == position) return
        if (!challengeMode && delta > 0 && position.stage == ReelStage.Shot &&
            startsGroup((position.shotIndex + 1) % shotCount)) advancedBrowseGroup = true
        resetPrediction()
        if (automatic && target.stage == ReelStage.Shot) pulseShotId = reelShots?.getOrNull(target.shotIndex)?.entry?.id
        position = target
    }

    fun browse(player: String?) {
        resetPrediction()
        challengeMode = false
        stats = EightBallReelStats()
        selectedPlayer = player
        advancedBrowseGroup = false
        position = ReelPosition(ReelStage.Player)
    }

    fun startChallenge(alias: String) {
        val available = loadedShots ?: return
        challengeError = null
        daily.open(alias) { active ->
            if (position.stage != ReelStage.ChallengeEntry) return@open
            if (active.challenge.shotIds.any { id -> available.none { it.entry.id == id } }) {
                challengeError = "Today's challenge contains unavailable videos. Please browse the available shots and try the next daily challenge after midnight UTC."
            } else {
                resetPrediction()
                challengeMode = true
                position = ReelPosition(if (active.complete) ReelStage.ChallengeResults else ReelStage.Player, active.answers.size)
            }
        }
    }

    fun leaveChallenge() {
        resetPrediction()
        challengeMode = false
        position = ReelPosition(ReelStage.ModeChoice)
    }

    fun nextChallengeShot() {
        if (position.stage != ReelStage.Shot || selectedBall == null || daily.busy) return
        moveReel(1)
    }

    // Warm only the current layout while its player intro is visible, not the whole library.
    LaunchedEffect(currentShot?.entry?.imagePath) {
        currentShot?.let { preloadLayoutImage(it.entry.imagePath) }
    }

    // While revealing, warm just the next layout, never the next clip or the full library.
    LaunchedEffect(position, selectedBall, challengeMode) {
        if (position.stage == ReelStage.Shot && selectedBall != null) {
            val next = reelShots?.getOrNull(position.shotIndex + 1)
                ?: if (!challengeMode) reelShots?.firstOrNull() else null
            next?.let { preloadLayoutImage(it.entry.imagePath) }
        }
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

    fun showAnswer(correct: Boolean, chosen: Int, coordinate: Boolean = false) {
        if(selectedBall!=null || resultFlash!=null) return
        answerClock?.pause()
        if(coordinate) coordinateResult=correct
        feedback=buildEightBallReelFeedback(correct,currentShot?.meta?.isBreak==true)
        if(!challengeMode) stats=stats.next(correct)
        resultFlash=correct
        playPredictionFeedback(correct)
        revealTimer=window.setTimeout({
            selectedBall=chosen
            resultFlash=null
            videoEnded=false
            videoPaused=false
            revealTimer=null
        },800)
    }

    fun confirmPoint() {
        if (selectedBall != null || resultFlash!=null || (challengeMode && (!daily.clockStarted || daily.busy))) return
        val ballSelection=currentShot?.meta?.ballSelection
        val target = ballSelection?.imageTarget ?: currentShot?.meta?.target ?: return
        val point = pointSelection ?: return
        val picked=ballSelection?.ballAt(point)
        if(ballSelection!=null && picked==null) return
        primePredictionAudio()
        if(challengeMode) {
            val shotId=currentShot?.entry?.id ?: return
            daily.answerBall(shotId,picked?.id ?: return) {
                daily.attempt?.answers?.find {it.shotId==shotId}?.let {answer ->
                    showAnswer(answer.correct,answer.selectedBall,coordinate=true)
                }
            }
        } else {
            val correct=if(ballSelection!=null) picked?.id==ballSelection.correctBallId else target.contains(point)
            showAnswer(correct,-2,coordinate=true)
        }
    }

    LaunchedEffect(challengeMode, position, daily.attempt?.answers) {
        if (challengeMode && position.stage == ReelStage.Shot) {
            daily.attempt?.answers?.find { it.shotId == currentShot?.entry?.id }?.let { answer ->
                if (selectedBall == null && resultFlash==null) {
                    selectedBall = answer.selectedBall
                    if(currentShot?.meta?.ballSelection!=null) coordinateResult=answer.correct
                    feedback = buildEightBallReelFeedback(answer.correct, currentShot?.meta?.isBreak == true)
                }
            }
        }
    }

    val scrollEnabled = loadedShots!=null && resultFlash==null && shareShot==null &&
        position.stage !in listOf(ReelStage.ModeChoice,ReelStage.PlayerChoice,ReelStage.ChallengeEntry,ReelStage.ChallengeResults)
    val canScrollNext = scrollEnabled && (!challengeMode || (!daily.busy && (position.stage!=ReelStage.Shot || selectedBall!=null)))
    val canScrollPrevious = scrollEnabled && !challengeMode && position.stage!=ReelStage.Welcome

    EightBallReelsShell(
        pager = pager,
        pageKey = position,
        previousKey = if(canScrollPrevious) destination(-1) else null,
        nextKey = if(canScrollNext) destination(1) else null,
        scrollLocked = resultFlash!=null || shareShot!=null || (challengeMode && daily.busy),
        onSettled = { moveReel(it, animate=false, automatic=pager.settledAutomatically) },
        preview = { value ->
            val target = value as ReelPosition
            val shot = reelShots?.getOrNull(target.shotIndex)
            ReelPreview(target, shot, players[shot?.meta?.player?.trim()?.lowercase()], challengeMode, displayedStats)
        },
        onHome = if(position.stage==ReelStage.Welcome) null else ({
            if(!daily.busy && resultFlash==null) {
                resetPrediction()
                challengeMode=false
                position=ReelPosition()
            }
        }),
        homeEnabled = !daily.busy && resultFlash==null && shareShot==null,
        showSound = position.stage==ReelStage.Shot,
        onNext = { moveReel(1) },
        onPrevious = { moveReel(-1) },
        canNext = canScrollNext,
        canPrevious = canScrollPrevious,
    ) {
        key(challengeMode, position.stage, if(position.stage in listOf(ReelStage.Player,ReelStage.Shot)) currentShot?.entry?.id else null) {
            when {
                loadError != null -> EightBallReelMessage(loadError ?: "Could not load shots.")
                loadedShots?.isEmpty() == true -> EightBallReelMessage("No shots.")
                position.stage == ReelStage.Welcome && (initialShotId == null || loadedShots != null) -> EightBallReelIntro(
                    onStart = { moveReel(1) },
                    canStart = loadedShots != null,
                )
                loadedShots == null -> PoolLoading("Loading shots…")
                position.stage == ReelStage.ModeChoice -> EightBallModeChoice(
                    onDailyChallenge = { position = ReelPosition(ReelStage.ChallengeEntry) },
                    onBrowse = { position = ReelPosition(ReelStage.PlayerChoice) },
                )
                position.stage == ReelStage.PlayerChoice -> EightBallPlayerChoice(
                    players = availablePlayers,
                    profiles = players,
                    onSelected = { browse(it) },
                    onBack = { position = ReelPosition(ReelStage.ModeChoice) },
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
                    onBrowse = { leaveChallenge(); position = ReelPosition(ReelStage.PlayerChoice) },
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
                        pulseOnReady = pulseShotId == currentShot.entry.id,
                        player = players[currentShot.meta.player.trim().lowercase()],
                        selectedBall = selectedBall,
                        feedback = feedback,
                        videoEnded = videoEnded,
                        videoPaused = videoPaused,
                        stats = displayedStats,
                        liked = currentShot.entry.id in likedShotIds,
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
                        onBallSelected = { ball ->
                            if (selectedBall != null || resultFlash!=null) return@EightBallReelShot
                            primePredictionAudio()
                            if (challengeMode) {
                                val active = daily.attempt ?: return@EightBallReelShot
                                if (active.currentShotId != currentShot.entry.id) return@EightBallReelShot
                                daily.answer(currentShot.entry.id, ball) {
                                    val answer = daily.attempt?.answers?.find { it.shotId == currentShot.entry.id }
                                    if (answer != null) {
                                        showAnswer(answer.correct,answer.selectedBall)
                                    }
                                }
                                return@EightBallReelShot
                            }
                            val correct = ball == currentShot.meta.correctAnswer
                            showAnswer(correct,ball)
                        },
                        onVideoElement = { element ->
                            if (element != null) videoElement = element
                            else if (videoElement?.getAttribute("src") == BasePath.prependTo(currentShot.entry.videoPath)) videoElement = null
                        },
                        onVideoEnded = {
                            videoEnded = true
                            videoPaused = false
                            if (!challengeMode && selectedBall != null) {
                                moveReel(1, automatic = true)
                            }
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
                        inputEnabled = resultFlash==null && (!challengeMode || (daily.clockStarted && !daily.busy)),
                        clockRunning = challengeMode && daily.clockStarted,
                        onLayoutReady = { if (challengeMode) daily.startShot(currentShot.entry.id) },
                        requestError = if (challengeMode) daily.error else null,
                        requestBusy = challengeMode && daily.busy,
                        onRetry = { daily.retryRequest() },
                        onChallengeNext = if (challengeMode) ({ nextChallengeShot() }) else null,
                        challengeLastShot = challengeMode && position.shotIndex == reelShots?.lastIndex,
                        pointSelection = pointSelection,
                        coordinateResult = coordinateResult,
                        onPointSelected = { pointSelection = it },
                        onPointConfirmed = { confirmPoint() },
                    )
                }
            }
        }

        resultFlash?.let { PredictionFeedback(it) }

        if (challengeMode && (position.stage == ReelStage.Player || position.stage == ReelStage.Shot) && currentShot != null && shareShot == null) {
            EightBallChallengeHeader(position.shotIndex + 1, reelShots.orEmpty().size, onLeave = { if(!daily.busy && resultFlash==null) leaveChallenge() })
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
