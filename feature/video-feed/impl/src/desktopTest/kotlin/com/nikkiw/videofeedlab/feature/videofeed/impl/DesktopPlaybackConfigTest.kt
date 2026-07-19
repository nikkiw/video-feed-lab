package com.nikkiw.videofeedlab.feature.videofeed.impl

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class DesktopPlaybackConfigTest {
    @Test
    fun keepsActiveNetworkAndLiveCachingIndependent() {
        val options =
            DesktopPlaybackConfig(
                networkCachingMs = 700,
                liveCachingMs = 900,
                standbyNetworkCachingMs = 2_500,
                standbyLiveCachingMs = 2_700,
                fileCachingMs = 150,
            ).mediaOptions(isStandby = false)

        assertContentEquals(
            arrayOf(
                ":network-caching=700",
                ":live-caching=900",
                ":file-caching=150",
                ":input-repeat=65535",
                ":no-video-title-show",
            ),
            options,
        )
    }

    @Test
    fun usesExplicitStandbyCachingWithoutMagicValues() {
        val options =
            DesktopPlaybackConfig(
                networkCachingMs = 700,
                liveCachingMs = 900,
                standbyNetworkCachingMs = 2_500,
                standbyLiveCachingMs = 2_700,
                fileCachingMs = 150,
            ).mediaOptions(isStandby = true)

        assertContentEquals(
            arrayOf(
                ":network-caching=2500",
                ":live-caching=2700",
                ":file-caching=150",
                ":input-repeat=65535",
                ":no-video-title-show",
            ),
            options,
        )
    }

    @Test
    fun rejectsNegativeStandbyCaching() {
        assertFailsWith<IllegalArgumentException> {
            DesktopPlaybackConfig(standbyNetworkCachingMs = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopPlaybackConfig(standbyLiveCachingMs = -1)
        }
    }
}
