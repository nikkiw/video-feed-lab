package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.catalog.network.NetworkCatalogFeed
import com.nikkiw.videofeedlab.shared.catalog.network.NetworkCatalogItem
import com.nikkiw.videofeedlab.shared.catalog.network.NetworkPlaybackSource
import com.nikkiw.videofeedlab.shared.model.StreamType
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoCatalogMapperTest {
    private val mapper =
        VideoCatalogMapper(
            urlResolver = MediaLabUrlResolver(MediaLabEnvironment(host = "lab-host")),
            playbackVariantPolicy = RoundRobinPlaybackVariantPolicy(),
        )

    @Test
    fun mapsCatalogSourcesAndProfilesInRoundRobinOrder() {
        val items = mapper.map(feed(item("one"), item("two"), item("three")))

        assertEquals(StreamType.HLS, items[0].source.streamType)
        assertEquals("http://lab-host:18081/vfl/media/one/hls/master.m3u8", items[0].source.uri)
        assertEquals(StreamType.DASH, items[1].source.streamType)
        assertEquals("http://lab-host:18083/vfl/media/two/dash/manifest.mpd", items[1].source.uri)
        assertEquals(StreamType.PROGRESSIVE, items[2].source.streamType)
        assertEquals("http://lab-host:18087/vfl/media/three/progressive/video.mp4", items[2].source.uri)
        assertEquals("http://lab-host:18081/img/one.webp", items[0].images.posterUrl)
    }

    @Test
    fun resolvesRootRelativeUrlsAgainstAbsoluteCatalogBaseOrigin() {
        val mapped = mapper.map(feed(item("one"), baseUrl = "https://cdn.example/catalog/v1"))

        assertEquals("https://cdn.example/vfl/media/one/hls/master.m3u8", mapped.single().source.uri)
        assertEquals("https://cdn.example/img/one.webp", mapped.single().images.posterUrl)
    }

    private fun feed(
        vararg items: NetworkCatalogItem,
        baseUrl: String = "",
    ) = NetworkCatalogFeed(version = 1, baseUrl = baseUrl, items = items.toList())

    private fun item(id: String) =
        NetworkCatalogItem(
            id = id,
            title = id,
            posterUrl = "/img/$id.webp",
            thumbnailUrl = "/img/$id-thumbnail.webp",
            playback =
                listOf(
                    NetworkPlaybackSource("hls", url = "/vfl/media/$id/hls/master.m3u8"),
                    NetworkPlaybackSource("dash", url = "/vfl/media/$id/dash/manifest.mpd"),
                    NetworkPlaybackSource("progressive", url = "/vfl/media/$id/progressive/video.mp4"),
                ),
        )
}
