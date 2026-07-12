package com.nikkiw.videofeedlab.shared.model

data class VideoItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val source: PlaybackSource,
    val thumbnailUrl: String,
    val timeBasedThumbnailUrlTemplate: String? = null,
    val images: VideoImageSet =
        VideoImageSet(
            posterUrl = thumbnailUrl,
            thumbnailUrl = thumbnailUrl,
        ),
)

data class VideoImageSet(
    val posterUrl: String,
    val thumbnailUrl: String,
    val blurredPosterUrl: String? = null,
    val avifPosterUrl: String? = null,
)

/** Returns the best poster for [positionMs]. Templates use `{timeSeconds}` as a placeholder. */
fun VideoItem.thumbnailUrlAt(positionMs: Long): String =
    timeBasedThumbnailUrlTemplate
        ?.replace("{timeSeconds}", (positionMs.coerceAtLeast(0L) / 1_000.0).toString())
        ?: images.posterUrl

data class PlaybackSource(
    val uri: String,
    val streamType: StreamType,
)

enum class StreamType {
    HLS,
    DASH,
    PROGRESSIVE,
}
