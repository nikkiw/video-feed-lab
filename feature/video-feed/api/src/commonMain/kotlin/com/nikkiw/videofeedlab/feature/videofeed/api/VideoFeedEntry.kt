package com.nikkiw.videofeedlab.feature.videofeed.api

import androidx.compose.runtime.Composable

interface VideoFeedEntry {
    @Composable
    fun Content(onBack: (() -> Unit)?)
}
