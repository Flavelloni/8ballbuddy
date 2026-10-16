package org.example.eighballpatterns.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.varabyte.kobweb.core.Page
import kotlinx.browser.window
import org.example.eighballpatterns.browser.copyEightBallShareUrl
import org.example.eighballpatterns.browser.currentEightBallReelsShotId
import org.example.eighballpatterns.browser.currentEightBallReelsPlayerIntro
import org.example.eighballpatterns.browser.shareEightBallShotNative
import org.example.eighballpatterns.browser.updateEightBallReelsUrl
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

    LaunchedEffect(Unit) {
        ensureEightBallReelsStyles()
        loadPlayerProfiles { players = it }
        loadEightBallShots(
            onLoaded = {
                shots = it
                val requestedIndex = it.indexOfFirst { shot -> shot.entry.id == initialShotId }
                if (requestedIndex >= 0) {
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
    val currentShot = loadedShots?.getOrNull(position.shotIndex)

    LaunchedEffect(position, loadedShots) {
        if (loadedShots == null) return@LaunchedEffect
        val shot = if (position.stage == ReelStage.Welcome) null else currentShot
        updateEightBallReelsUrl(shot?.entry?.id, position.stage == ReelStage.Player)
    }

    fun resetPrediction() {
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
        val shotCount = loadedShots?.size ?: return
        if (shotCount == 0 || isAdvancing || imageFullscreen || shareShot != null) return
        isAdvancing = true
        resetPrediction()
        position = if (delta > 0) position.next(shotCount) else position.previous()
        window.setTimeout({ isAdvancing = false }, EIGHT_BALL_SLIDE_DURATION_MS)
    }

    LaunchedEffect(position, selectedBall, videoElement) {
        if (selectedBall != null) {
            videoElement?.let { video ->
                video.currentTime = 0.0
                videoPaused = false
                video.play()
            }
        }
    }

    fun toggleVideoPlayback() {
        val video = videoElement ?: return
        if (videoEnded) {
            video.currentTime = 0.0
            videoEnded = false
            videoPaused = false
            video.play()
        } else if (video.paused) {
            videoPaused = false
            video.play()
        } else {
            videoPaused = true
            video.pause()
        }
    }

    EightBallReelsShell(
        onNext = { moveReel(1) },
        onPrevious = { moveReel(-1) },
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
        when {
            loadError != null -> EightBallReelMessage(loadError ?: "Could not load shots.")
            loadedShots?.isEmpty() == true -> EightBallReelMessage("No shots.")
            position.stage == ReelStage.Welcome && (initialShotId == null || loadedShots != null) -> EightBallReelIntro(
                onStart = { moveReel(1) },
                canStart = loadedShots != null,
            )
            loadedShots == null -> EightBallReelMessage("Loading...")
            position.stage == ReelStage.Player && currentShot != null -> EightBallPlayerIntro(
                shot = currentShot,
                player = players[currentShot.meta.player.trim().lowercase()],
            )
            currentShot != null -> {
                EightBallReelShot(
                    shot = currentShot,
                    selectedBall = selectedBall,
                    feedback = feedback,
                    videoEnded = videoEnded,
                    videoPaused = videoPaused,
                    stats = stats,
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
                    onBallSelected = { ball ->
                        if (selectedBall != null) return@EightBallReelShot
                        val correct = ball == currentShot.meta.ball
                        selectedBall = ball
                        videoEnded = false
                        videoPaused = false
                        feedback = buildEightBallReelFeedback(correct)
                        stats = stats.next(correct)
                        videoElement?.let { video ->
                            video.currentTime = 0.0
                            video.play()
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
                )

                if (imageFullscreen) {
                    EightBallFullscreenImage(
                        shot = currentShot,
                        onClose = { imageFullscreen = false }
                    )
                }
            }
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
