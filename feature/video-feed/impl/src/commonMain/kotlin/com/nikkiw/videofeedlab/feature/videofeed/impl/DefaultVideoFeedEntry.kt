package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedLaunchParams
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory
import com.nikkiw.videofeedlab.shared.catalog.VideoRepository

class DefaultVideoFeedEntry(
    private val componentContext: ComponentContext,
    private val storeFactory: StoreFactory,
    private val repository: VideoRepository,
    private val launchParams: FeedLaunchParams,
) : VideoFeedEntry {
    @Composable
    override fun Content(onBack: (() -> Unit)?) {
        val component =
            remember(componentContext, launchParams) {
                DefaultVideoFeedComponent(componentContext, storeFactory, repository, launchParams)
            }
        PlatformVideoFeedScreen(component = component, onBack = onBack)
    }
}

class DefaultVideoFeedEntryFactory(
    private val storeFactory: StoreFactory,
    private val repository: VideoRepository,
) : VideoFeedEntryFactory {
    override fun create(
        componentContext: ComponentContext,
        launchParams: FeedLaunchParams,
    ): VideoFeedEntry = DefaultVideoFeedEntry(componentContext, storeFactory, repository, launchParams)
}
