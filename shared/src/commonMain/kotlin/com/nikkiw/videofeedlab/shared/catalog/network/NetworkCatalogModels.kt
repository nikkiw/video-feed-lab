package com.nikkiw.videofeedlab.shared.catalog.network

import kotlinx.serialization.Serializable

@Serializable
data class NetworkCatalogFeed(
    val version: Int,
    val baseUrl: String = "",
    val items: List<NetworkCatalogItem> = emptyList(),
)

@Serializable
data class NetworkCatalogItem(
    val id: String,
    val title: String,
    val posterUrl: String,
    val thumbnailUrl: String,
    val blurredPosterUrl: String? = null,
    val avifPosterUrl: String? = null,
    val lqip: String? = null,
    val progressiveUrl: String? = null,
    val hlsUrl: String? = null,
    val dashUrl: String? = null,
    val playback: List<NetworkPlaybackSource> = emptyList(),
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val hasAudio: Boolean? = null,
    val storyboard: NetworkStoryboard? = null,
)

@Serializable
data class NetworkPlaybackSource(
    val type: String,
    val mimeType: String? = null,
    val url: String,
)

@Serializable
data class NetworkStoryboard(
    val url: String,
    val intervalSeconds: Int,
    val frameCount: Int,
    val width: Int,
)
