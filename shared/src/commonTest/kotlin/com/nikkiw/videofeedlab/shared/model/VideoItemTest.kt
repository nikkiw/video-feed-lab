package com.nikkiw.videofeedlab.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoItemTest {
    private val source = PlaybackSource("https://example.com/video.m3u8", StreamType.HLS)

    @Test
    fun timeBasedThumbnailUsesSavedPosition() {
        val item =
            VideoItem(
                id = "video",
                title = "Video",
                subtitle = "Test",
                source = source,
                thumbnailUrl = "https://example.com/default.jpg",
                timeBasedThumbnailUrlTemplate = "https://example.com/frame.jpg?time={timeSeconds}",
            )

        assertEquals("https://example.com/frame.jpg?time=12.345", item.thumbnailUrlAt(12_345L))
    }

    @Test
    fun staticThumbnailIsFallbackWhenApiIsUnavailable() {
        val item =
            VideoItem(
                id = "video",
                title = "Video",
                subtitle = "Test",
                source = source,
                thumbnailUrl = "https://example.com/default.jpg",
            )

        assertEquals(item.thumbnailUrl, item.thumbnailUrlAt(90_000L))
    }
}
