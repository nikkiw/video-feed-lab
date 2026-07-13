package com.nikkiw.videofeedlab.feature.videofeed.api

import com.arkivanov.decompose.ComponentContext

fun interface VideoFeedEntryFactory {
    fun create(componentContext: ComponentContext): VideoFeedEntry
}
