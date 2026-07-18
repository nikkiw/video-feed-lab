package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.Composable
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent

@Composable
internal expect fun PlatformVideoFeedScreen(
    component: VideoFeedComponent,
    onBack: (() -> Unit)? = null,
)
