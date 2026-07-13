package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.catalog.network.KtorVideoCatalogApi
import com.nikkiw.videofeedlab.shared.catalog.network.VideoCatalogApi
import com.nikkiw.videofeedlab.shared.catalog.network.createCatalogHttpClient
import com.nikkiw.videofeedlab.shared.catalog.network.createCatalogJson
import org.koin.dsl.module
import org.koin.dsl.onClose

val catalogModule =
    module {
        single { createCatalogJson() }
        single { createCatalogHttpClient(get()) } onClose { client -> client?.close() }
        single { MediaLabEnvironment() }
        single { MediaLabUrlResolver(get()) }
        single { VideoFeedLabRoutes(get()) }
        single<PlaybackVariantPolicy> { RoundRobinPlaybackVariantPolicy() }
        single { VideoCatalogMapper(get(), get()) }
        single<VideoCatalogApi> { KtorVideoCatalogApi(get(), get()) }
        single<VideoRepository> { DefaultVideoRepository(get(), get()) }
    }
