package com.nikkiw.videofeedlab.shared.catalog.network

import com.nikkiw.videofeedlab.shared.catalog.MediaLabEnvironment
import com.nikkiw.videofeedlab.shared.catalog.MediaLabNetworkProfile
import com.nikkiw.videofeedlab.shared.catalog.MediaLabUrlResolver
import com.nikkiw.videofeedlab.shared.catalog.VideoFeedLabRoutes
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorVideoCatalogApiTest {
    @Test
    fun requestsProfileFeedAndDecodesCatalogWhileIgnoringUnknownFields() =
        runTest {
            var requestedUrl = ""
            val engine =
                MockEngine { request ->
                    requestedUrl = request.url.toString()
                    respond(
                        content = FEED_JSON,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client =
                HttpClient(engine) {
                    configureCatalogHttpClient(createCatalogJson())
                }
            val api =
                KtorVideoCatalogApi(
                    client,
                    VideoFeedLabRoutes(MediaLabUrlResolver(MediaLabEnvironment("lab-host"))),
                )

            val feed = api.fetchCatalog(MediaLabNetworkProfile.LTE)

            assertEquals("http://lab-host:18083/vfl/api/feed", requestedUrl)
            assertEquals("video", feed.items.single().id)
            assertEquals("data:image/webp;base64,lqip-content", feed.items.single().lqip)
            client.close()
        }

    private companion object {
        const val FEED_JSON =
            """
            {
              "version": 1,
              "baseUrl": "",
              "items": [
                {
                  "id": "video",
                  "title": "Video",
                  "posterUrl": "/poster.webp",
                  "thumbnailUrl": "/thumbnail.webp",
                  "lqip": "data:image/webp;base64,lqip-content",
                  "hlsUrl": "/video.m3u8",
                  "unknownField": true
                }
              ]
            }
            """
    }
}
