package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.catalog.network.NetworkCatalogFeed
import com.nikkiw.videofeedlab.shared.catalog.network.NetworkCatalogItem
import com.nikkiw.videofeedlab.shared.model.PlaybackSource
import com.nikkiw.videofeedlab.shared.model.StreamType
import com.nikkiw.videofeedlab.shared.model.VideoImageSet
import com.nikkiw.videofeedlab.shared.model.VideoItem

class VideoCatalogMapper(
    private val urlResolver: MediaLabUrlResolver,
    private val playbackVariantPolicy: PlaybackVariantPolicy,
) {
    fun map(feed: NetworkCatalogFeed): List<VideoItem> =
        feed.items.mapIndexed { index, item ->
            val variant = playbackVariantPolicy.variantFor(index)
            val playbackUrl = item.playbackUrl(variant.streamType)
            val resolvedPlaybackUrl =
                urlResolver.resolve(
                    url = playbackUrl,
                    profile = variant.networkProfile,
                    baseUrl = feed.baseUrl,
                )
            val images =
                VideoImageSet(
                    posterUrl = item.posterUrl.resolveImage(feed.baseUrl),
                    thumbnailUrl = item.thumbnailUrl.resolveImage(feed.baseUrl),
                    blurredPosterUrl = item.blurredPosterUrl?.resolveImage(feed.baseUrl),
                    avifPosterUrl = item.avifPosterUrl?.resolveImage(feed.baseUrl),
                )

            VideoItem(
                id = "vfl-${item.id}",
                title = item.title,
                subtitle = "${variant.streamType.name} · ${variant.networkProfile.name.lowercase()}",
                source = PlaybackSource(uri = resolvedPlaybackUrl, streamType = variant.streamType),
                thumbnailUrl = images.thumbnailUrl,
                images = images,
            )
        }

    private fun NetworkCatalogItem.playbackUrl(streamType: StreamType): String {
        val type = streamType.name.lowercase()
        return playback.firstOrNull { it.type.lowercase() == type }?.url
            ?: when (streamType) {
                StreamType.HLS -> hlsUrl
                StreamType.DASH -> dashUrl
                StreamType.PROGRESSIVE -> progressiveUrl
            }
            ?: error("Catalog item '$id' has no $type playback URL")
    }

    private fun String.resolveImage(baseUrl: String): String =
        urlResolver.resolve(
            url = this,
            profile = MediaLabNetworkProfile.CLEAN,
            baseUrl = baseUrl,
        )
}
