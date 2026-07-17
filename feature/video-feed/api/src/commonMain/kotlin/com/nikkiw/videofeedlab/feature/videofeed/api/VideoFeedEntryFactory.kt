package com.nikkiw.videofeedlab.feature.videofeed.api

import com.arkivanov.decompose.ComponentContext

interface VideoFeedEntryFactory {
    fun create(
        componentContext: ComponentContext,
        launchParams: FeedLaunchParams = FeedLaunchParams(),
    ): VideoFeedEntry
}
