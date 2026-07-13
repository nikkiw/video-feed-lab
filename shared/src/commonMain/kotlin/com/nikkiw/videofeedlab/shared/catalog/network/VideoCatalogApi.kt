package com.nikkiw.videofeedlab.shared.catalog.network

import com.nikkiw.videofeedlab.shared.catalog.MediaLabNetworkProfile
import com.nikkiw.videofeedlab.shared.catalog.VideoFeedLabRoutes
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

interface VideoCatalogApi {
    suspend fun fetchCatalog(profile: MediaLabNetworkProfile): NetworkCatalogFeed
}

class KtorVideoCatalogApi(
    private val client: HttpClient,
    private val routes: VideoFeedLabRoutes,
) : VideoCatalogApi {
    override suspend fun fetchCatalog(profile: MediaLabNetworkProfile): NetworkCatalogFeed {
        return client
            .get(routes.feed(profile))
            .body()
    }
}
