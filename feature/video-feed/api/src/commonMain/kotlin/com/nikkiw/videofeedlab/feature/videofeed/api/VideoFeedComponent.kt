package com.nikkiw.videofeedlab.feature.videofeed.api

import com.arkivanov.decompose.value.Value
import com.nikkiw.videofeedlab.shared.model.VideoItem

interface VideoFeedComponent {
    val models: Value<Model>

    data class Model(
        val items: List<VideoItem>,
        val activeIndex: Int,
        val isMuted: Boolean,
        val isPlaying: Boolean,
        val debugState: PlaybackDebugState,
    )

    fun onPageSelected(index: Int)

    fun onToggleMute()

    fun onTogglePlay()
}
