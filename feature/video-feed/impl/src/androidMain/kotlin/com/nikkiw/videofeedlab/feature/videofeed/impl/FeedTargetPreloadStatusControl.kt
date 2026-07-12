package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import kotlin.math.abs

internal class FeedTargetPreloadStatusControl(
    private val isLowRam: Boolean,
    var currentPlayingIndex: Int = 0,
) : TargetPreloadStatusControl<Int, DefaultPreloadManager.PreloadStatus> {
    override fun getTargetPreloadStatus(index: Int): DefaultPreloadManager.PreloadStatus {
        val distance = index - currentPlayingIndex

        if (isLowRam) {
            return when {
                distance == 1 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_SOURCE_PREPARED
                else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
            }
        }

        return when {
            distance == 1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(3_000L)
            distance == -1 -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(1_500L)
            abs(distance) == 2 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_TRACKS_SELECTED
            abs(distance) <= 4 -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_SOURCE_PREPARED
            else -> DefaultPreloadManager.PreloadStatus.PRELOAD_STATUS_NOT_PRELOADED
        }
    }
}
