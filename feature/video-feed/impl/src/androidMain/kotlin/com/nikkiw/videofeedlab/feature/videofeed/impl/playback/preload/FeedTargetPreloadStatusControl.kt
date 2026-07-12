package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.preload

import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl

internal class FeedTargetPreloadStatusControl(
    private val isLowRam: Boolean,
    var currentPlayingIndex: Int = 0,
    var isScrollingForward: Boolean = true,
) : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
    override fun getTargetPreloadStatus(index: Int): DefaultPreloadManager.PreloadStatus {
        val distance = index - currentPlayingIndex
        val directionalDistance = if (isScrollingForward) distance else -distance
        if (isLowRam) {
            return when (directionalDistance) {
                1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(1_500L)
                else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
        }
        return when (directionalDistance) {
            1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(4_000L)
            2 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_TRACKS_SELECTED
            -1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(1_500L)
            else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
        }
    }
}
