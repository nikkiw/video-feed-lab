package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.ui.graphics.ImageBitmap

/** Observable state snapshot emitted by [DesktopPlaybackCoordinator]. */
internal data class DesktopPlaybackState(
    val activeIndex: Int = -1,
    val pages: Map<Int, DesktopPagePlaybackState> = emptyMap(),
) {
    val activePage: DesktopPagePlaybackState?
        get() = pages[activeIndex]
}

internal fun DesktopPlaybackState.withoutScrollFrames(): DesktopPlaybackState =
    copy(pages = pages.mapValues { (_, page) -> page.copy(scrollFrame = null) })

internal data class DesktopPagePlaybackState(
    val surfaceId: Int? = null,
    val posterUrl: String,
    val scrollFrame: ImageBitmap? = null,
    val frameReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val standbyReady: Boolean = false,
    val startupSource: DesktopStartupSource = DesktopStartupSource.COLD,
    val startupTimeMs: Long? = null,
    val preloadTimeMs: Long? = null,
    val rebufferCount: Int = 0,
    val errorCount: Int = 0,
    val errorMessage: String? = null,
)

internal enum class DesktopStartupSource {
    COLD,
    STANDBY,
}

internal enum class DesktopScrollDirection {
    INITIAL,
    FORWARD,
    BACKWARD,
}

internal fun scrollDirection(
    from: Int,
    to: Int,
): DesktopScrollDirection =
    when {
        from < 0 -> DesktopScrollDirection.INITIAL
        to > from -> DesktopScrollDirection.FORWARD
        else -> DesktopScrollDirection.BACKWARD
    }

internal fun preferredAdjacentIndex(
    centerIndex: Int,
    direction: DesktopScrollDirection,
    lastIndex: Int,
): Int? {
    val preferred =
        if (direction == DesktopScrollDirection.BACKWARD) centerIndex - 1 else centerIndex + 1
    val fallback =
        if (direction == DesktopScrollDirection.BACKWARD) centerIndex + 1 else centerIndex - 1
    return listOf(preferred, fallback).firstOrNull { it in 0..lastIndex }
}

internal enum class DesktopSlotPhase {
    EMPTY,
    PREPARING,
    READY,
    PLAYING,
    ENDED,
    FAILED,
    RELEASED,
}

internal fun canPromoteWithoutBuffering(
    assignedIndex: Int?,
    requestedIndex: Int,
    phase: DesktopSlotPhase,
    frameReady: Boolean,
): Boolean =
    assignedIndex == requestedIndex &&
        phase == DesktopSlotPhase.READY &&
        frameReady

internal fun shouldPauseStandbyAfterProgress(
    pauseAfterWarmup: Boolean,
    playbackTimeMs: Long,
): Boolean = pauseAfterWarmup && playbackTimeMs > 0L

internal fun hasAdvancedForPresentation(
    playbackTimeMs: Long,
    baselineTimeMs: Long,
): Boolean = playbackTimeMs - baselineTimeMs >= MIN_PRESENTATION_PROGRESS_MS

internal const val MIN_PRESENTATION_PROGRESS_MS = 100L
