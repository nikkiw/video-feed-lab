package com.nikkiw.videofeedlab.feature.videofeed.impl

/**
 * LibVLC buffering policy for the desktop short-video feed.
 *
 * These values affect transient LibVLC buffers. They do not implement
 * a persistent on-disk media cache.
 */
internal data class DesktopPlaybackConfig(
    val networkCachingMs: Int = DEFAULT_NETWORK_CACHING_MS,
    val liveCachingMs: Int = DEFAULT_LIVE_CACHING_MS,
    val fileCachingMs: Int = DEFAULT_FILE_CACHING_MS,
) {
    init {
        require(networkCachingMs >= 0) {
            "networkCachingMs must not be negative"
        }
        require(liveCachingMs >= 0) {
            "liveCachingMs must not be negative"
        }
        require(fileCachingMs >= 0) {
            "fileCachingMs must not be negative"
        }
    }

    fun mediaOptions(): Array<String> =
        arrayOf(
            ":network-caching=$networkCachingMs",
            ":live-caching=$liveCachingMs",
            ":file-caching=$fileCachingMs",
            ":no-video-title-show",
        )

    private companion object {
        /*
         * The previous 1,200 ms policy was conservative for ordinary playback,
         * but noticeably slow for a short-video feed.
         *
         * These values must later be validated against Clean/LTE/Flaky
         * Universal Media Lab profiles.
         */
        const val DEFAULT_NETWORK_CACHING_MS = 600
        const val DEFAULT_LIVE_CACHING_MS = 600
        const val DEFAULT_FILE_CACHING_MS = 200
    }
}
