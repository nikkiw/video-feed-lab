package com.nikkiw.videofeedlab.feature.videofeed.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopSourceModeTest {
    @Test
    fun `compatibility mode rewrites HLS to progressive`() {
        assertEquals(
            "http://127.0.0.1:18081/vfl/media/asset-1/progressive",
            resolveDesktopPlaybackUri(
                sourceUri = "http://127.0.0.1:18081/vfl/media/asset-1/hls/master.m3u8",
                sourceMode = DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
            ),
        )
    }

    @Test
    fun `compatibility mode rewrites DASH and preserves query`() {
        assertEquals(
            "http://127.0.0.1:18083/vfl/media/asset-2/progressive?token=test",
            resolveDesktopPlaybackUri(
                sourceUri =
                    "http://127.0.0.1:18083/vfl/media/asset-2/dash/manifest.mpd?token=test",
                sourceMode = DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
            ),
        )
    }

    @Test
    fun `compatibility mode keeps progressive and unknown routes unchanged`() {
        val progressive = "http://127.0.0.1:18081/vfl/media/asset-3/progressive"
        val faultRoute =
            "http://127.0.0.1:18081/vfl/fault/ttfb/1000/asset-3/hls/master.m3u8"

        assertEquals(
            progressive,
            resolveDesktopPlaybackUri(
                sourceUri = progressive,
                sourceMode = DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
            ),
        )
        assertEquals(
            faultRoute,
            resolveDesktopPlaybackUri(
                sourceUri = faultRoute,
                sourceMode = DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
            ),
        )
    }

    @Test
    fun `adaptive mode keeps the catalog source`() {
        val source = "http://127.0.0.1:18081/vfl/media/asset-4/hls/master.m3u8"

        assertEquals(
            source,
            resolveDesktopPlaybackUri(
                sourceUri = source,
                sourceMode = DesktopSourceMode.ORIGINAL_ADAPTIVE,
            ),
        )
    }

    @Test
    fun `adaptive environment value is explicit`() {
        assertEquals(
            DesktopSourceMode.ORIGINAL_ADAPTIVE,
            desktopSourceModeFromEnvironment("adaptive"),
        )
        assertEquals(
            DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
            desktopSourceModeFromEnvironment(null),
        )
        assertEquals(
            DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
            desktopSourceModeFromEnvironment("progressive"),
        )
    }

    @Test
    fun `standby preload waits for stable active playback`() {
        assertFalse(
            canStartStandbyPreload(
                sourceMode = DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
                frameReady = true,
                isBuffering = false,
                playbackTimeMs = 749L,
                baselineTimeMs = 0L,
                minimumStablePlaybackMs = 750L,
            ),
        )
        assertTrue(
            canStartStandbyPreload(
                sourceMode = DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
                frameReady = true,
                isBuffering = false,
                playbackTimeMs = 750L,
                baselineTimeMs = 0L,
                minimumStablePlaybackMs = 750L,
            ),
        )
    }

    @Test
    fun `standby preload is disabled while buffering and in adaptive mode`() {
        assertFalse(
            canStartStandbyPreload(
                sourceMode = DesktopSourceMode.COMPATIBILITY_PROGRESSIVE,
                frameReady = true,
                isBuffering = true,
                playbackTimeMs = 2_000L,
                baselineTimeMs = 0L,
                minimumStablePlaybackMs = 750L,
            ),
        )
        assertFalse(
            canStartStandbyPreload(
                sourceMode = DesktopSourceMode.ORIGINAL_ADAPTIVE,
                frameReady = true,
                isBuffering = false,
                playbackTimeMs = 2_000L,
                baselineTimeMs = 0L,
                minimumStablePlaybackMs = 750L,
            ),
        )
    }
}
