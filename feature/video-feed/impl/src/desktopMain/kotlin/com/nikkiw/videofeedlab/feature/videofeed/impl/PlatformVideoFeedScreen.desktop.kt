package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent

@Composable
internal actual fun PlatformVideoFeedScreen(component: VideoFeedComponent) {
    val model by component.models.subscribeAsState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Video Feed Lab (Desktop)")
            Button(onClick = { component.onToggleMute() }) {
                Text(if (model.isMuted) "Unmute" else "Mute")
            }
        }

        Text("Active Index: ${model.activeIndex}")

        model.items.forEachIndexed { index, item ->
            val isActive = index == model.activeIndex
            val backgroundColor = if (isActive) Color.LightGray else Color.Transparent
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .clickable { component.onPageSelected(index) }
                        .padding(8.dp),
            ) {
                Text("• ${item.title} — ${item.source.streamType}")
            }
        }
    }
}
