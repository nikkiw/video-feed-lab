package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.shared.catalog.DemoVideoCatalog

class DefaultVideoFeedEntry(private val componentContext: ComponentContext) : VideoFeedEntry {
    @Composable
    override fun Content() {
        val component =
            remember(componentContext) {
                DefaultVideoFeedComponent(componentContext, DemoVideoCatalog.items)
            }
        PlatformVideoFeedScreen(component = component)
    }
}

fun createVideoFeedEntry(componentContext: ComponentContext): VideoFeedEntry = DefaultVideoFeedEntry(componentContext)
