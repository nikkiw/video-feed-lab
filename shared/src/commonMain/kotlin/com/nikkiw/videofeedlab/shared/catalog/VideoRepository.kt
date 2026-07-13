package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.catalog.network.VideoCatalogApi
import com.nikkiw.videofeedlab.shared.model.VideoItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CatalogRequest(
    val feedProfile: MediaLabNetworkProfile = MediaLabNetworkProfile.CLEAN,
)

interface VideoRepository {
    suspend fun getVideos(
        request: CatalogRequest = CatalogRequest(),
        forceRefresh: Boolean = false,
    ): List<VideoItem>
}

class DefaultVideoRepository(
    private val api: VideoCatalogApi,
    private val mapper: VideoCatalogMapper,
) : VideoRepository {
    private val cacheMutex = Mutex()
    private val cachedItems = mutableMapOf<CatalogRequest, List<VideoItem>>()

    override suspend fun getVideos(
        request: CatalogRequest,
        forceRefresh: Boolean,
    ): List<VideoItem> =
        cacheMutex.withLock {
            if (!forceRefresh) {
                cachedItems[request]?.let { return@withLock it }
            }

            val items = mapper.map(api.fetchCatalog(request.feedProfile))
            cachedItems[request] = items
            items
        }
}
