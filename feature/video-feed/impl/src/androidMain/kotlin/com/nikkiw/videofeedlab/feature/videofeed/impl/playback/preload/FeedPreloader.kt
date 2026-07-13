package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.preload

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics.ScrollDirection
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.policy.PlaybackPolicy
import kotlin.math.max
import kotlin.math.min

internal interface FeedPreloader {
    fun updateWindow(
        centerIndex: Int,
        direction: ScrollDirection,
    )

    fun mediaSourceFor(index: Int): MediaSource?

    fun release()
}

@OptIn(UnstableApi::class)
internal class Media3FeedPreloader(
    private val manager: DefaultPreloadManager,
    private val statusControl: FeedTargetPreloadStatusControl,
    private val mediaItems: List<MediaItem>,
    private val policy: PlaybackPolicy,
) : FeedPreloader {
    private val registeredIndices = mutableSetOf<Int>()
    private var released = false

    override fun updateWindow(
        centerIndex: Int,
        direction: ScrollDirection,
    ) {
        if (released || centerIndex !in mediaItems.indices) return
        statusControl.currentPlayingIndex = centerIndex
        statusControl.isScrollingForward = direction != ScrollDirection.BACKWARD
        manager.setCurrentPlayingIndex(centerIndex)

        val backwardCount =
            if (direction == ScrollDirection.BACKWARD) policy.forwardPreloadCount else policy.backwardPreloadCount
        val forwardCount =
            if (direction == ScrollDirection.FORWARD || direction == ScrollDirection.INITIAL) {
                policy.forwardPreloadCount
            } else {
                policy.backwardPreloadCount
            }
        val start = max(0, centerIndex - backwardCount)
        val end = min(mediaItems.lastIndex, centerIndex + forwardCount)
        for (index in start..end) {
            if (registeredIndices.add(index)) manager.add(mediaItems[index], index)
        }
        manager.invalidate()
    }

    override fun mediaSourceFor(index: Int): MediaSource? =
        if (!released && index in mediaItems.indices) manager.getMediaSource(mediaItems[index]) else null

    override fun release() {
        if (released) return
        released = true
        registeredIndices.clear()
        manager.release()
    }
}
