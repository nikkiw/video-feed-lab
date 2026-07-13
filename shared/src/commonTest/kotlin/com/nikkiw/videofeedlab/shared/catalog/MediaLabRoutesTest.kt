package com.nikkiw.videofeedlab.shared.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaLabRoutesTest {
    private val routes =
        VideoFeedLabRoutes(
            MediaLabUrlResolver(MediaLabEnvironment(host = "lab-host")),
        )

    @Test
    fun networkProfileChangesOnlyThePort() {
        assertEquals(
            "http://lab-host:18083/vfl/media/demo/hls/master.m3u8",
            routes.hls("demo", MediaLabNetworkProfile.LTE),
        )
        assertEquals(
            "http://lab-host:18087/vfl/media/demo/hls/master.m3u8",
            routes.hls("demo", MediaLabNetworkProfile.FLAKY),
        )
    }

    @Test
    fun imageRoutesUseFixedSizeBuckets() {
        assertEquals(
            "http://lab-host:18081/vfl/poster/demo/placeholder.webp",
            routes.posterPlaceholder("demo"),
        )
        assertEquals(
            "http://lab-host:18081/vfl/poster/demo/thumbnail.webp",
            routes.posterThumbnail("demo"),
        )
        assertEquals(
            "http://lab-host:18081/vfl/poster/demo/feed.webp",
            routes.posterFeed("demo"),
        )
    }

    @Test
    fun transportAndServerDelayCanBeCombined() {
        assertEquals(
            "http://lab-host:18083/vfl/fault/ttfb/1000/demo/hls/master.m3u8",
            routes.hlsWithTtfb("demo", 1_000, MediaLabNetworkProfile.LTE),
        )
        assertFailsWith<IllegalArgumentException> {
            routes.hlsWithTtfb("demo", 1_500, MediaLabNetworkProfile.LTE)
        }
    }
}
