package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.model.PlaybackSource
import com.nikkiw.videofeedlab.shared.model.StreamType
import com.nikkiw.videofeedlab.shared.model.VideoImageSet
import com.nikkiw.videofeedlab.shared.model.VideoItem

object DemoVideoCatalog {
    val assetIds = listOf(
        "dance-neon",
        "girl-portrait",
        "woman-smile",
        "man-running",
        "people-active",
        "couple-laugh",
        "woman-dancing",
        "urban-lights",
        "skater-jump",
        "city-street",
        "surf-ocean",
        "active-sports",
        "neon-signs",
        "nature-waterfall",
        "urban-runner",
        "scenery-mountains",
        "dj-turntable",
        "cyberpunk-city",
        "street-dance",
        "aesthetic-sunset"
    )

    val items: List<VideoItem> = create()

    fun create(
        environment: MediaLabEnvironment = MediaLabEnvironment(),
    ): List<VideoItem> {
        val routes = VideoFeedLabRoutes(MediaLabUrlResolver(environment))
        
        return assetIds.mapIndexed { index, assetId ->
            // Cycle stream types: HLS, DASH, Progressive
            val streamType = when (index % 3) {
                0 -> StreamType.HLS
                1 -> StreamType.DASH
                else -> StreamType.PROGRESSIVE
            }
            
            // Cycle network profiles to test different environments
            val profile = when (index % 3) {
                0 -> MediaLabNetworkProfile.CLEAN
                1 -> MediaLabNetworkProfile.LTE
                else -> MediaLabNetworkProfile.FLAKY
            }
            
            val uri = when (streamType) {
                StreamType.HLS -> routes.hls(assetId, profile)
                StreamType.DASH -> routes.dash(assetId, profile)
                StreamType.PROGRESSIVE -> routes.progressive(assetId, profile)
            }
            
            // Generate a human-readable title from the asset ID
            val title = assetId.split("-", "_")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            
            val subtitle = "${streamType.name} · ${profile.name.lowercase()}"
            
            val images = VideoImageSet(
                posterUrl = routes.posterFeed(assetId),
                thumbnailUrl = routes.posterThumbnail(assetId),
                blurredPosterUrl = routes.posterPlaceholder(assetId),
                avifPosterUrl = routes.posterAvif(assetId),
            )
            
            video(
                id = "vfl-$assetId",
                title = title,
                subtitle = subtitle,
                uri = uri,
                streamType = streamType,
                images = images,
            )
        }
    }

    private fun video(
        id: String,
        title: String,
        subtitle: String,
        uri: String,
        streamType: StreamType,
        images: VideoImageSet,
    ): VideoItem =
        VideoItem(
            id = id,
            title = title,
            subtitle = subtitle,
            source = PlaybackSource(uri, streamType),
            thumbnailUrl = images.thumbnailUrl,
            images = images,
        )
}
