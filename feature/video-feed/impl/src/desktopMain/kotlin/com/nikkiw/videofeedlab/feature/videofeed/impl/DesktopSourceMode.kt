package com.nikkiw.videofeedlab.feature.videofeed.impl

internal enum class DesktopSourceMode {
    COMPATIBILITY_PROGRESSIVE,
    ORIGINAL_ADAPTIVE,
    ;

    val supportsStandbyPreload: Boolean
        get() = this == COMPATIBILITY_PROGRESSIVE
}

internal fun desktopSourceModeFromEnvironment(): DesktopSourceMode =
    desktopSourceModeFromEnvironment(System.getenv(DESKTOP_SOURCE_MODE_ENV))

internal fun desktopSourceModeFromEnvironment(rawValue: String?): DesktopSourceMode {
    return when (rawValue?.trim()?.lowercase()) {
        "adaptive",
        "original-adaptive",
        "original_adaptive",
        -> DesktopSourceMode.ORIGINAL_ADAPTIVE

        else -> DesktopSourceMode.COMPATIBILITY_PROGRESSIVE
    }
}

internal fun resolveDesktopPlaybackUri(
    sourceUri: String,
    sourceMode: DesktopSourceMode,
): String {
    val isOriginalOrNoMarker =
        sourceMode == DesktopSourceMode.ORIGINAL_ADAPTIVE ||
            MEDIA_ROUTE_MARKER !in sourceUri

    return if (isOriginalOrNoMarker) {
        sourceUri
    } else {
        sourceUri.replace(ADAPTIVE_MEDIA_SUFFIX) { match ->
            "/progressive${match.groupValues[1]}"
        }
    }
}

internal fun canStartStandbyPreload(
    sourceMode: DesktopSourceMode,
    frameReady: Boolean,
    isBuffering: Boolean,
    playbackTimeMs: Long,
    baselineTimeMs: Long,
    minimumStablePlaybackMs: Long,
): Boolean {
    if (!sourceMode.supportsStandbyPreload || !frameReady || isBuffering) return false

    val stablePlaybackMs = (playbackTimeMs - baselineTimeMs).coerceAtLeast(0L)
    return stablePlaybackMs >= minimumStablePlaybackMs
}

internal const val DESKTOP_SOURCE_MODE_ENV = "VIDEO_FEED_LAB_DESKTOP_SOURCE_MODE"

private const val MEDIA_ROUTE_MARKER = "/vfl/media/"

private val ADAPTIVE_MEDIA_SUFFIX =
    Regex("""/(?:hls/master\.m3u8|dash/manifest\.mpd)(\?.*)?$""")
