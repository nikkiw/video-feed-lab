package com.nikkiw.videofeedlab.feature.videofeed.api

data class PlaybackDebugState(
    val videoId: String? = null,
    val startupTimeMs: Long? = null,
    val rebufferCount: Int = 0,
    val bitrateKbps: Int? = null,
    val resolution: String? = null,
    val isPlaying: Boolean = false,
)
