package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics

import kotlin.math.ceil

internal enum class StartupSource {
    STANDBY_READY,
    STANDBY_PREPARING,
    PRELOADED_MEDIA_SOURCE,
    NETWORK_OR_CACHE,
}

internal enum class ScrollDirection {
    INITIAL,
    FORWARD,
    BACKWARD,
}

internal data class StartupSample(
    val videoId: String,
    val durationMs: Long,
    val source: StartupSource,
    val direction: ScrollDirection,
    val cacheRead: Boolean,
)

internal data class PlaybackAnalyticsSnapshot(
    val videoStarts: Int = 0,
    val startupSamples: List<StartupSample> = emptyList(),
    val totalRebuffers: Int = 0,
    val totalErrors: Int = 0,
    val totalDroppedFrames: Int = 0,
) {
    val p50StartupTimeMs: Long
        get() = percentile(startupSamples.map(StartupSample::durationMs), 50)

    val p95StartupTimeMs: Long
        get() = percentile(startupSamples.map(StartupSample::durationMs), 95)
}

internal interface PlaybackAnalytics {
    val snapshot: PlaybackAnalyticsSnapshot

    fun trackVideoStart(videoId: String)

    fun trackStartup(sample: StartupSample)

    fun trackRebuffer(videoId: String)

    fun trackError(
        videoId: String,
        message: String,
    )

    fun trackDroppedFrames(count: Int)
}

internal fun percentile(
    values: List<Long>,
    percentile: Int,
): Long {
    if (values.isEmpty()) return 0L
    val rank = ceil(percentile.coerceIn(1, 100) / 100.0 * values.size).toInt()
    return values.sorted()[rank.coerceIn(1, values.size) - 1]
}

internal class InMemoryPlaybackAnalytics : PlaybackAnalytics {
    private var mutableSnapshot = PlaybackAnalyticsSnapshot()

    override val snapshot: PlaybackAnalyticsSnapshot
        get() = mutableSnapshot

    override fun trackVideoStart(videoId: String) {
        mutableSnapshot = mutableSnapshot.copy(videoStarts = mutableSnapshot.videoStarts + 1)
        println("[PlaybackAnalytics] VIDEO_START video=$videoId total=${mutableSnapshot.videoStarts}")
    }

    override fun trackStartup(sample: StartupSample) {
        mutableSnapshot = mutableSnapshot.copy(startupSamples = mutableSnapshot.startupSamples + sample)
        println(
            "[PlaybackAnalytics] STARTUP video=${sample.videoId} time=${sample.durationMs}ms " +
                "source=${sample.source} direction=${sample.direction} cacheRead=${sample.cacheRead} " +
                "n=${snapshot.startupSamples.size} p50=${snapshot.p50StartupTimeMs}ms " +
                "p95=${snapshot.p95StartupTimeMs}ms",
        )
    }

    override fun trackRebuffer(videoId: String) {
        mutableSnapshot = mutableSnapshot.copy(totalRebuffers = mutableSnapshot.totalRebuffers + 1)
        println("[PlaybackAnalytics] REBUFFER video=$videoId total=${mutableSnapshot.totalRebuffers}")
    }

    override fun trackError(
        videoId: String,
        message: String,
    ) {
        mutableSnapshot = mutableSnapshot.copy(totalErrors = mutableSnapshot.totalErrors + 1)
        println("[PlaybackAnalytics] ERROR video=$videoId message=$message total=${mutableSnapshot.totalErrors}")
    }

    override fun trackDroppedFrames(count: Int) {
        mutableSnapshot = mutableSnapshot.copy(totalDroppedFrames = mutableSnapshot.totalDroppedFrames + count)
        println("[PlaybackAnalytics] DROPPED count=$count total=${mutableSnapshot.totalDroppedFrames}")
    }
}
