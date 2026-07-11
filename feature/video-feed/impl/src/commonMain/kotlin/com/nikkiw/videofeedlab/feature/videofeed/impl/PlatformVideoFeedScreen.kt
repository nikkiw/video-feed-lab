package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.Composable
import com.nikkiw.videofeedlab.shared.model.VideoItem

@Composable
internal expect fun PlatformVideoFeedScreen(items: List<VideoItem>)
