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
    val standbyNetworkCachingMs: Int = DEFAULT_STANDBY_NETWORK_CACHING_MS,
    val standbyLiveCachingMs: Int = DEFAULT_STANDBY_LIVE_CACHING_MS,
    val fileCachingMs: Int = DEFAULT_FILE_CACHING_MS,
) {
    init {
        require(networkCachingMs >= 0) {
            "networkCachingMs must not be negative"
        }
        require(liveCachingMs >= 0) {
            "liveCachingMs must not be negative"
        }
        require(standbyNetworkCachingMs >= 0) {
            "standbyNetworkCachingMs must not be negative"
        }
        require(standbyLiveCachingMs >= 0) {
            "standbyLiveCachingMs must not be negative"
        }
        require(fileCachingMs >= 0) {
            "fileCachingMs must not be negative"
        }
    }

    fun mediaOptions(isStandby: Boolean): Array<String> {
        val selectedNetworkCachingMs =
            if (isStandby) standbyNetworkCachingMs else networkCachingMs
        val selectedLiveCachingMs =
            if (isStandby) standbyLiveCachingMs else liveCachingMs
        return arrayOf(
            ":network-caching=$selectedNetworkCachingMs",
            ":live-caching=$selectedLiveCachingMs",
            ":file-caching=$fileCachingMs",
            // Native repeat is the single loop owner. Do not restart playback
            // manually from the LibVLC finished callback.
            ":input-repeat=65535",
            ":no-video-title-show",
        )
    }

    private companion object {
        /*
         * 600 ms exposed a repeatable underrun about one second after startup
         * for Media Lab DASH/LTE playback. 800 ms gives a safe margin above that
         * threshold while cutting ~700 ms off cold-start latency visible on
         * HLS/CLEAN and other fast profiles.
         */
        const val DEFAULT_NETWORK_CACHING_MS = 800
        const val DEFAULT_LIVE_CACHING_MS = 800
        const val DEFAULT_STANDBY_NETWORK_CACHING_MS = 3_000
        const val DEFAULT_STANDBY_LIVE_CACHING_MS = 3_000
        const val DEFAULT_FILE_CACHING_MS = 200
    }
}
