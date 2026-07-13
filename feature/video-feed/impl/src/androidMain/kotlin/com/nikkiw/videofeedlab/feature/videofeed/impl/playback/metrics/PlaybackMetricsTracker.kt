package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics

import android.os.SystemClock
import androidx.media3.common.Format
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.cache.CacheReadCounter
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.SlotAssignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun interface PlaybackClock {
    fun elapsedRealtimeMs(): Long
}

internal object AndroidPlaybackClock : PlaybackClock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}

internal interface PlaybackMetricsTracker {
    val debugState: StateFlow<PlaybackDebugState>

    fun startupRequested(
        assignment: SlotAssignment,
        source: StartupSource,
        direction: ScrollDirection,
    )

    fun firstFrameRendered(
        assignment: SlotAssignment,
        format: Format?,
    )

    fun playingChanged(
        videoId: String,
        playing: Boolean,
    )

    fun buffering(videoId: String)

    fun error(
        videoId: String,
        message: String,
    )

    fun droppedFrames(count: Int)

    fun release()
}

internal class DefaultPlaybackMetricsTracker(
    private val clock: PlaybackClock,
    private val cacheReadCounter: CacheReadCounter,
    private val analytics: PlaybackAnalytics,
    private val isLowRamDevice: Boolean,
    private val onDebugState: (PlaybackDebugState) -> Unit,
) : PlaybackMetricsTracker {
    private data class PendingStartup(
        val assignment: SlotAssignment,
        val requestedAtMs: Long,
        val source: StartupSource,
        val direction: ScrollDirection,
        val cachedBytesAtStart: Long,
    )

    private val mutableDebugState = MutableStateFlow(PlaybackDebugState(isLowRamDevice = isLowRamDevice))
    private var pendingStartup: PendingStartup? = null
    private var rebufferCount = 0
    private var released = false

    override val debugState: StateFlow<PlaybackDebugState> = mutableDebugState.asStateFlow()

    override fun startupRequested(
        assignment: SlotAssignment,
        source: StartupSource,
        direction: ScrollDirection,
    ) {
        if (released) return
        pendingStartup =
            PendingStartup(
                assignment = assignment,
                requestedAtMs = clock.elapsedRealtimeMs(),
                source = source,
                direction = direction,
                cachedBytesAtStart = cacheReadCounter.bytesRead(),
            )
        rebufferCount = 0
        publish(
            PlaybackDebugState(
                videoId = assignment.mediaId,
                startupSource = source.name,
                startupDirection = direction.name,
                isLowRamDevice = isLowRamDevice,
            ).withAnalytics(analytics.snapshot),
        )
    }

    override fun firstFrameRendered(
        assignment: SlotAssignment,
        format: Format?,
    ) {
        if (released) return
        val pending = pendingStartup?.takeIf { it.assignment == assignment } ?: return
        val durationMs = clock.elapsedRealtimeMs() - pending.requestedAtMs
        val cacheRead = cacheReadCounter.bytesRead() > pending.cachedBytesAtStart
        analytics.trackStartup(
            StartupSample(assignment.mediaId, durationMs, pending.source, pending.direction, cacheRead),
        )
        pendingStartup = null
        publish(
            mutableDebugState.value
                .copy(
                    startupTimeMs = durationMs,
                    cacheRead = cacheRead,
                    bitrateKbps = format?.bitrate?.takeIf { it > 0 }?.div(1_000),
                    resolution =
                        format?.takeIf { it.width > 0 && it.height > 0 }?.let { "${it.width}×${it.height}" },
                ).withAnalytics(analytics.snapshot),
        )
    }

    override fun playingChanged(
        videoId: String,
        playing: Boolean,
    ) {
        if (released) return
        if (playing) analytics.trackVideoStart(videoId)
        publish(mutableDebugState.value.copy(isPlaying = playing).withAnalytics(analytics.snapshot))
    }

    override fun buffering(videoId: String) {
        if (released || pendingStartup != null) return
        rebufferCount++
        analytics.trackRebuffer(videoId)
        publish(
            mutableDebugState.value
                .copy(rebufferCount = rebufferCount)
                .withAnalytics(analytics.snapshot),
        )
    }

    override fun error(
        videoId: String,
        message: String,
    ) {
        if (released) return
        analytics.trackError(videoId, message)
        publish(mutableDebugState.value.withAnalytics(analytics.snapshot))
    }

    override fun droppedFrames(count: Int) {
        if (released) return
        analytics.trackDroppedFrames(count)
        publish(mutableDebugState.value.withAnalytics(analytics.snapshot))
    }

    override fun release() {
        if (released) return
        released = true
        pendingStartup = null
    }

    private fun publish(state: PlaybackDebugState) {
        mutableDebugState.value = state
        onDebugState(state)
    }

    private fun PlaybackDebugState.withAnalytics(snapshot: PlaybackAnalyticsSnapshot): PlaybackDebugState =
        copy(
            sampleCount = snapshot.startupSamples.size,
            videoStarts = snapshot.videoStarts,
            p50StartupTimeMs = snapshot.p50StartupTimeMs,
            p95StartupTimeMs = snapshot.p95StartupTimeMs,
            totalRebuffers = snapshot.totalRebuffers,
            totalErrors = snapshot.totalErrors,
            totalDroppedFrames = snapshot.totalDroppedFrames,
        )
}
