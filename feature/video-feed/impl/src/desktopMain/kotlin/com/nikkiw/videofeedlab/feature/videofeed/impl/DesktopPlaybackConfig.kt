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

    fun mediaOptions(isStandby: Boolean): Array<String> {
        val cachingMs = if (isStandby) 3000 else networkCachingMs
        return arrayOf(
            ":network-caching=$cachingMs",
            ":live-caching=$cachingMs",
            ":file-caching=$fileCachingMs",
            // NOTE: Native loop avoids the 1-2s decoder teardown delay, but visually
            // jerks sharply to the beginning. The ultimate solution is to implement a
            // local HTTP caching proxy to save network segments to disk and serve them
            // instantly, achieving parity with ExoPlayer's SimpleCache.
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
        const val DEFAULT_FILE_CACHING_MS = 200
    }
}
