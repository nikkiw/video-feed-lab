package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.shared.model.VideoItem

/**
 * Maps the actual LibVLC-backed state to the platform-neutral feature state.
 *
 * Aggregate startup percentiles are intentionally not calculated here because
 * DesktopPagePlaybackState stores only the latest startup sample per page.
 */
internal fun DesktopPlaybackState.toPlaybackDebugState(items: List<VideoItem>): PlaybackDebugState {
    val activePage = activePage

    return PlaybackDebugState(
        videoId = items.getOrNull(activeIndex)?.id,
        startupTimeMs = activePage?.startupTimeMs,
        rebufferCount = activePage?.rebufferCount ?: 0,
        isPlaying = activePage?.isPlaying == true,
        startupSource = activePage?.startupSource?.debugName,
        totalRebuffers = pages.values.sumOf(DesktopPagePlaybackState::rebufferCount),
        totalErrors = pages.values.sumOf(DesktopPagePlaybackState::errorCount),
    )
}

private val DesktopStartupSource.debugName: String
    get() =
        when (this) {
            DesktopStartupSource.COLD -> "cold"
            DesktopStartupSource.STANDBY -> "standby"
        }
