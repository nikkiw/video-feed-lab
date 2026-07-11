package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikkiw.videofeedlab.shared.model.VideoItem

@Composable
internal actual fun PlatformVideoFeedScreen(items: List<VideoItem>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Video Feed Lab")
        Text("The playback experiment is Android-specific because it uses Media3 / ExoPlayer.")
        items.forEach { item ->
            Text("• ${item.title} — ${item.source.streamType}")
        }
    }
}
