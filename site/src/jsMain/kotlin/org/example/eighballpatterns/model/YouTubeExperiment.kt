package org.example.eighballpatterns.model

// Parked experiment: leave off for normal local-MP4 playback. To revisit, enable
// this flag and uncomment the YouTube experiment entry in ChallengeReels.kt.
internal const val ENABLE_YOUTUBE_EXPERIMENT = false

internal data class YouTubeShotSegment(val shotId: String, val videoId: String, val start: Double, val end: Double) {
    init {
        require(Regex("[A-Za-z0-9_-]{11}").matches(videoId))
        require(start.isFinite() && end.isFinite() && start >= 0 && end > start)
    }

    fun hasEnded(time: Double): Boolean = time.isFinite() && time >= end
}

// Original broadcast seconds = local clip timing + the downloaded segment's 18,960s offset.
// Source: MagicCitySlate/.../052056/timing.json, clip_start_seconds=340.4, clip_end_seconds=348.5.
internal val youtubeExperimentShot = YouTubeShotSegment("AMDgls4nJhQ/052056", "AMDgls4nJhQ", 19300.4, 19308.5)
