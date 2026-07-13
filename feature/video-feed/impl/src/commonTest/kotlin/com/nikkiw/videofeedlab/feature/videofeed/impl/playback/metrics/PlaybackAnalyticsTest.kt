package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackAnalyticsTest {
    @Test
    fun percentileUsesNearestRank() {
        val values = (1L..100L).toList()
        assertEquals(50L, percentile(values, 50))
        assertEquals(95L, percentile(values, 95))
    }

    @Test
    fun p95OnSmallSampleIsTheSlowestStartup() {
        assertEquals(6_000L, percentile(listOf(400L, 900L, 6_000L), 95))
    }

    @Test
    fun emptySampleHasZeroPercentile() {
        assertEquals(0L, percentile(emptyList(), 95))
    }
}
