package com.nikkiw.videofeedlab.feature.videofeed.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPlaybackGenerationTest {
    @Test
    fun acceptsOnlyEventsFromCurrentAssignmentGeneration() {
        val assignment =
            DesktopSlotAssignment(
                index = 2,
                videoId = "video-2",
                sourceUri = "https://example.test/video-2.m3u8",
                generation = 7L,
            )

        assertTrue(matchesGeneration(assignment, eventGeneration = 7L))
        assertFalse(matchesGeneration(assignment, eventGeneration = 6L))
        assertFalse(matchesGeneration(null, eventGeneration = 7L))
    }

    @Test
    fun pendingMediaActionCarriesTheAssignmentGeneration() {
        val action =
            PendingPlayerAction.PlayMrl(
                mrl = "https://example.test/video.mpd",
                generation = 11L,
                options = arrayOf(":network-caching=800"),
            )

        assertEquals(11L, action.generation)
    }
}
