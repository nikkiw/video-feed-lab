package com.nikkiw.videofeedlab.feature.videofeed.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopPlaybackPolicyTest {
    @Test
    fun derivesScrollDirectionFromSelection() {
        assertEquals(DesktopScrollDirection.INITIAL, scrollDirection(-1, 0))
        assertEquals(DesktopScrollDirection.FORWARD, scrollDirection(2, 3))
        assertEquals(DesktopScrollDirection.BACKWARD, scrollDirection(3, 2))
    }

    @Test
    fun preloadsInScrollDirection() {
        assertEquals(3, preferredAdjacentIndex(2, DesktopScrollDirection.FORWARD, lastIndex = 5))
        assertEquals(1, preferredAdjacentIndex(2, DesktopScrollDirection.BACKWARD, lastIndex = 5))
    }

    @Test
    fun fallsBackToAvailableNeighborAtFeedEdges() {
        assertEquals(1, preferredAdjacentIndex(0, DesktopScrollDirection.BACKWARD, lastIndex = 5))
        assertEquals(4, preferredAdjacentIndex(5, DesktopScrollDirection.FORWARD, lastIndex = 5))
        assertNull(preferredAdjacentIndex(0, DesktopScrollDirection.INITIAL, lastIndex = 0))
    }
}
