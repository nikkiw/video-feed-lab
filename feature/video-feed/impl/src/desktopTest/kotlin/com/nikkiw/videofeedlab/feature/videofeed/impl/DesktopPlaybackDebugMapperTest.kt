package com.nikkiw.videofeedlab.feature.videofeed.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPlaybackDebugMapperTest {
    @Test
    fun `maps active LibVLC page to feature debug state`() {
        val state =
            DesktopPlaybackState(
                activeIndex = 1,
                pages =
                    mapOf(
                        0 to
                            DesktopPagePlaybackState(
                                posterUrl = "poster-0",
                                rebufferCount = 2,
                                errorCount = 1,
                            ),
                        1 to
                            DesktopPagePlaybackState(
                                posterUrl = "poster-1",
                                frameReady = true,
                                isPlaying = true,
                                startupSource = DesktopStartupSource.STANDBY,
                                startupTimeMs = 430L,
                                rebufferCount = 3,
                                errorCount = 2,
                            ),
                    ),
            )

        val debug =
            state.toPlaybackDebugState(
                videoIds = listOf("video-0", "video-1"),
            )

        assertEquals("video-1", debug.videoId)
        assertEquals(430L, debug.startupTimeMs)
        assertEquals(3, debug.rebufferCount)
        assertEquals("standby", debug.startupSource)
        assertEquals(5, debug.totalRebuffers)
        assertEquals(3, debug.totalErrors)
        assertTrue(debug.isPlaying)
    }

    @Test
    fun `returns stopped debug state when no page is active`() {
        val debug =
            DesktopPlaybackState(
                activeIndex = -1,
                pages = emptyMap(),
            ).toPlaybackDebugState(
                videoIds = listOf("video-0"),
            )

        assertNull(debug.videoId)
        assertNull(debug.startupTimeMs)
        assertEquals(0, debug.rebufferCount)
        assertEquals(0, debug.totalRebuffers)
        assertEquals(0, debug.totalErrors)
        assertFalse(debug.isPlaying)
    }
}
