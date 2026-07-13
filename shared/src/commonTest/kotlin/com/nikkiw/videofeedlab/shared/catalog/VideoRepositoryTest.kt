package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.catalog.network.NetworkCatalogFeed
import com.nikkiw.videofeedlab.shared.catalog.network.NetworkCatalogItem
import com.nikkiw.videofeedlab.shared.catalog.network.NetworkPlaybackSource
import com.nikkiw.videofeedlab.shared.catalog.network.VideoCatalogApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoRepositoryTest {
    @Test
    fun cachesByRequestAndForceRefreshes() =
        runTest {
            val api = FakeVideoCatalogApi()
            val repository = repository(api)

            repository.getVideos()
            repository.getVideos()
            assertEquals(1, api.callCount)

            repository.getVideos(forceRefresh = true)
            assertEquals(2, api.callCount)

            repository.getVideos(CatalogRequest(MediaLabNetworkProfile.LTE))
            assertEquals(3, api.callCount)
        }

    @Test
    fun coalescesConcurrentRequestsThroughTheCacheLock() =
        runTest {
            val api = FakeVideoCatalogApi()
            val repository = repository(api)

            listOf(
                async { repository.getVideos() },
                async { repository.getVideos() },
            ).awaitAll()

            assertEquals(1, api.callCount)
        }

    private fun repository(api: VideoCatalogApi): VideoRepository =
        DefaultVideoRepository(
            api = api,
            mapper =
                VideoCatalogMapper(
                    MediaLabUrlResolver(MediaLabEnvironment(host = "lab-host")),
                    RoundRobinPlaybackVariantPolicy(),
                ),
        )

    private class FakeVideoCatalogApi : VideoCatalogApi {
        var callCount = 0
            private set

        override suspend fun fetchCatalog(profile: MediaLabNetworkProfile): NetworkCatalogFeed {
            callCount++
            return NetworkCatalogFeed(
                version = 1,
                items =
                    listOf(
                        NetworkCatalogItem(
                            id = "video",
                            title = "Video",
                            posterUrl = "/poster.webp",
                            thumbnailUrl = "/thumbnail.webp",
                            playback =
                                listOf(
                                    NetworkPlaybackSource(
                                        type = "hls",
                                        url = "/vfl/media/video/hls/master.m3u8",
                                    ),
                                ),
                        ),
                    ),
            )
        }
    }
}
