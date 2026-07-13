package com.nikkiw.videofeedlab.shared.catalog

internal expect val defaultMediaLabHost: String

enum class MediaLabNetworkProfile(
    val port: Int,
) {
    DYNAMIC(18080),
    CLEAN(18081),
    WIFI(18082),
    LTE(18083),
    SLOW_LTE(18084),
    THREE_G(18085),
    EDGE(18086),
    FLAKY(18087),
    OFFLINE(18088),
}

data class MediaLabEnvironment(
    val host: String = defaultMediaLabHost,
)

class MediaLabUrlResolver(
    private val environment: MediaLabEnvironment,
) {
    fun origin(profile: MediaLabNetworkProfile): String = "http://${environment.host}:${profile.port}"

    fun resolve(
        url: String,
        profile: MediaLabNetworkProfile,
        baseUrl: String = "",
    ): String =
        when {
            url.isAbsoluteHttpUrl() -> url
            baseUrl.isAbsoluteHttpUrl() -> baseUrl.resolveRelativeUrl(url)
            else -> {
                val path = "${baseUrl.trimEnd('/')}/${url.trimStart('/')}"
                "${origin(profile)}/${path.trimStart('/')}"
            }
        }

    private fun String.isAbsoluteHttpUrl(): Boolean = startsWith("http://") || startsWith("https://")

    private fun String.httpOrigin(): String {
        val authorityStart = indexOf("://") + 3
        val pathStart = indexOf('/', startIndex = authorityStart)
        return if (pathStart == -1) this else substring(0, pathStart)
    }

    private fun String.resolveRelativeUrl(relativeUrl: String): String =
        if (relativeUrl.startsWith('/')) {
            "${httpOrigin()}$relativeUrl"
        } else {
            "${trimEnd('/')}/$relativeUrl"
        }
}

class VideoFeedLabRoutes(
    private val resolver: MediaLabUrlResolver,
) {
    fun feed(profile: MediaLabNetworkProfile = MediaLabNetworkProfile.CLEAN): String {
        return resolver.resolve("/vfl/api/feed", profile)
    }

    fun hls(
        assetId: String,
        profile: MediaLabNetworkProfile,
    ): String = resolver.resolve("/vfl/media/$assetId/hls/master.m3u8", profile)

    fun dash(
        assetId: String,
        profile: MediaLabNetworkProfile,
    ): String = resolver.resolve("/vfl/media/$assetId/dash/manifest.mpd", profile)

    fun progressive(
        assetId: String,
        profile: MediaLabNetworkProfile,
    ): String = resolver.resolve("/vfl/media/$assetId/progressive", profile)

    fun posterPlaceholder(
        assetId: String,
        profile: MediaLabNetworkProfile = MediaLabNetworkProfile.CLEAN,
    ): String = resolver.resolve("/vfl/poster/$assetId/placeholder.webp", profile)

    fun posterThumbnail(
        assetId: String,
        profile: MediaLabNetworkProfile = MediaLabNetworkProfile.CLEAN,
    ): String = resolver.resolve("/vfl/poster/$assetId/thumbnail.webp", profile)

    fun posterFeed(
        assetId: String,
        profile: MediaLabNetworkProfile = MediaLabNetworkProfile.CLEAN,
    ): String = resolver.resolve("/vfl/poster/$assetId/feed.webp", profile)

    fun posterAvif(
        assetId: String,
        profile: MediaLabNetworkProfile = MediaLabNetworkProfile.CLEAN,
    ): String = resolver.resolve("/vfl/poster/$assetId/feed.avif", profile)

    fun hlsWithTtfb(
        assetId: String,
        delayMs: Int,
        profile: MediaLabNetworkProfile,
    ): String {
        require(delayMs in supportedTtfbDelays)
        return resolver.resolve("/vfl/fault/ttfb/$delayMs/$assetId/hls/master.m3u8", profile)
    }

    fun status(
        code: Int,
        profile: MediaLabNetworkProfile = MediaLabNetworkProfile.CLEAN,
    ): String {
        require(code in supportedStatusCodes)
        return resolver.resolve("/vfl/fault/status/$code", profile)
    }

    private companion object {
        val supportedTtfbDelays = setOf(200, 1_000, 3_000)
        val supportedStatusCodes = setOf(404, 429, 500, 503)
    }
}
