package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory
import com.nikkiw.videofeedlab.shared.catalog.VideoRepository

class DefaultVideoFeedEntry(
    private val componentContext: ComponentContext,
    private val storeFactory: StoreFactory,
    private val repository: VideoRepository,
) : VideoFeedEntry {
    @Composable
    override fun Content() {
        val component =
            remember(componentContext) {
                DefaultVideoFeedComponent(componentContext, storeFactory, repository)
            }
        PlatformVideoFeedScreen(component = component)
    }
}

class DefaultVideoFeedEntryFactory(
    private val storeFactory: StoreFactory,
    private val repository: VideoRepository,
) : VideoFeedEntryFactory {
    override fun create(componentContext: ComponentContext): VideoFeedEntry =
        DefaultVideoFeedEntry(componentContext, storeFactory, repository)
}
