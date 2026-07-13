package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics

import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.cache.CacheReadCounter
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.SlotAssignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackMetricsTrackerTest {
    private class FakeClock(var now: Long = 0L) : PlaybackClock {
        override fun elapsedRealtimeMs(): Long = now
    }

    private class FakeCache(var bytes: Long = 0L) : CacheReadCounter {
        override fun bytesRead(): Long = bytes
    }

    @Test
    fun staleFirstFrameDoesNotCompleteCurrentStartup() {
        val clock = FakeClock(100L)
        val analytics = InMemoryPlaybackAnalytics()
        val tracker = tracker(clock, FakeCache(), analytics)
        val old = SlotAssignment(1, "one", 1L)
        val current = SlotAssignment(2, "two", 2L)

        tracker.startupRequested(current, StartupSource.NETWORK_OR_CACHE, ScrollDirection.FORWARD)
        clock.now = 500L
        tracker.firstFrameRendered(old, null)

        assertEquals(0, analytics.snapshot.startupSamples.size)
        assertNull(tracker.debugState.value.startupTimeMs)
        tracker.firstFrameRendered(current, null)
        assertEquals(400L, analytics.snapshot.startupSamples.single().durationMs)
    }

    @Test
    fun cacheAndClockAreMeasuredDeterministically() {
        val clock = FakeClock(1_000L)
        val cache = FakeCache(10L)
        val analytics = InMemoryPlaybackAnalytics()
        val tracker = tracker(clock, cache, analytics)
        val assignment = SlotAssignment(0, "video", 7L)

        tracker.startupRequested(assignment, StartupSource.PRELOADED_MEDIA_SOURCE, ScrollDirection.INITIAL)
        clock.now = 1_250L
        cache.bytes = 50L
        tracker.firstFrameRendered(assignment, null)

        val sample = analytics.snapshot.startupSamples.single()
        assertEquals(250L, sample.durationMs)
        assertEquals(true, sample.cacheRead)
    }

    @Test
    fun callbacksAfterReleaseAreIgnoredAndReleaseIsIdempotent() {
        val analytics = InMemoryPlaybackAnalytics()
        val tracker = tracker(FakeClock(), FakeCache(), analytics)
        val assignment = SlotAssignment(0, "video", 1L)

        tracker.startupRequested(assignment, StartupSource.NETWORK_OR_CACHE, ScrollDirection.INITIAL)
        tracker.release()
        tracker.release()
        tracker.firstFrameRendered(assignment, null)
        tracker.playingChanged("video", true)

        assertEquals(PlaybackAnalyticsSnapshot(), analytics.snapshot)
    }

    private fun tracker(
        clock: PlaybackClock,
        cache: CacheReadCounter,
        analytics: PlaybackAnalytics,
    ): DefaultPlaybackMetricsTracker =
        DefaultPlaybackMetricsTracker(
            clock = clock,
            cacheReadCounter = cache,
            analytics = analytics,
            isLowRamDevice = false,
            onDebugState = {},
        )
}
