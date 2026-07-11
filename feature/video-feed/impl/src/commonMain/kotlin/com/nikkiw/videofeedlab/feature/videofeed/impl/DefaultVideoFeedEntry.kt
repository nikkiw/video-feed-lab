package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.Composable
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.shared.catalog.DemoVideoCatalog

class DefaultVideoFeedEntry : VideoFeedEntry {
    @Composable
    override fun content() {
        PlatformVideoFeedScreen(items = DemoVideoCatalog.items)
    }
}

fun createVideoFeedEntry(): VideoFeedEntry = DefaultVideoFeedEntry()
