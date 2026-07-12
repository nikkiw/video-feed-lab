package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AnalyticsManager {
    var isLowRamDevice by mutableStateOf(false)
    var videoStarts by mutableStateOf(0)
    var totalRebuffers by mutableStateOf(0)
    var totalErrors by mutableStateOf(0)
    val startupTimes = mutableStateListOf<Long>()

    val p50StartupTime: Long
        get() = getPercentile(50)

    val p95StartupTime: Long
        get() = getPercentile(95)

    private fun getPercentile(p: Int): Long {
        if (startupTimes.isEmpty()) return 0L
        val sorted = startupTimes.sorted()
        val index = (sorted.size * p / 100).coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    fun trackVideoStart(videoId: String) {
        videoStarts++
        println(
            "[EnterpriseAnalytics] 📊 Event: VIDEO_START | " +
                "Video: $videoId | TotalStarts: $videoStarts",
        )
    }

    fun trackStartupTime(
        videoId: String,
        startupTimeMs: Long,
    ) {
        startupTimes.add(startupTimeMs)
        println(
            "[EnterpriseAnalytics] 📊 Event: STARTUP_TIME | " +
                "Video: $videoId | Time: ${startupTimeMs}ms | " +
                "p50: ${p50StartupTime}ms | p95: ${p95StartupTime}ms",
        )
    }

    fun trackRebuffer(videoId: String) {
        totalRebuffers++
        println(
            "[EnterpriseAnalytics] 📊 Event: REBUFFER | " +
                "Video: $videoId | TotalRebuffers: $totalRebuffers",
        )
    }

    fun trackError(
        videoId: String,
        message: String,
    ) {
        totalErrors++
        println(
            "[EnterpriseAnalytics] 📊 Event: ERROR | " +
                "Video: $videoId | Msg: $message | TotalErrors: $totalErrors",
        )
    }
}
