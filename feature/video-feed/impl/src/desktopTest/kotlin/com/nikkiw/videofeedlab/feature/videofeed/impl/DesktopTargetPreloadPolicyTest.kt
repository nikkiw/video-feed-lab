package com.nikkiw.videofeedlab.feature.videofeed.impl

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopTargetPreloadPolicyTest {
    @Test
    fun acceptsAValidTargetPage() {
        assertTrue(
            shouldQueueTargetPreload(
                requestedIndex = 2,
                currentIndex = 1,
                lastIndex = 4,
                releaseRequested = false,
                supportsStandbyPreload = true,
            ),
        )
    }

    @Test
    fun rejectsCurrentOrOutOfRangePages() {
        assertFalse(
            shouldQueueTargetPreload(
                requestedIndex = 1,
                currentIndex = 1,
                lastIndex = 4,
                releaseRequested = false,
                supportsStandbyPreload = true,
            ),
        )
        assertFalse(
            shouldQueueTargetPreload(
                requestedIndex = 5,
                currentIndex = 1,
                lastIndex = 4,
                releaseRequested = false,
                supportsStandbyPreload = true,
            ),
        )
    }

    @Test
    fun rejectsRequestsDuringReleaseOrWithoutStandbySupport() {
        assertFalse(
            shouldQueueTargetPreload(
                requestedIndex = 2,
                currentIndex = 1,
                lastIndex = 4,
                releaseRequested = true,
                supportsStandbyPreload = true,
            ),
        )
        assertFalse(
            shouldQueueTargetPreload(
                requestedIndex = 2,
                currentIndex = 1,
                lastIndex = 4,
                releaseRequested = false,
                supportsStandbyPreload = false,
            ),
        )
    }
}
