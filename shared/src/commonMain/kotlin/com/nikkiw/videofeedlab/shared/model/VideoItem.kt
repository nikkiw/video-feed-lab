package com.nikkiw.videofeedlab.shared.model

data class VideoItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val source: PlaybackSource,
)

data class PlaybackSource(
    val uri: String,
    val streamType: StreamType,
)

enum class StreamType {
    HLS,
    DASH,
    PROGRESSIVE,
}
