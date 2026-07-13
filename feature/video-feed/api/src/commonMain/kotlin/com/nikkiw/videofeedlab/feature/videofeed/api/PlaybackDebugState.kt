package com.nikkiw.videofeedlab.feature.videofeed.api

data class PlaybackDebugState(
    val videoId: String? = null,
    val startupTimeMs: Long? = null,
    val rebufferCount: Int = 0,
    val bitrateKbps: Int? = null,
    val resolution: String? = null,
    val isPlaying: Boolean = false,
    val startupSource: String? = null,
    val startupDirection: String? = null,
    val cacheRead: Boolean = false,
    val sampleCount: Int = 0,
    val isLowRamDevice: Boolean = false,
    val videoStarts: Int = 0,
    val p50StartupTimeMs: Long = 0L,
    val p95StartupTimeMs: Long = 0L,
    val totalRebuffers: Int = 0,
    val totalErrors: Int = 0,
    val totalDroppedFrames: Int = 0,
)
