package com.nikkiw.videofeedlab.feature.videofeed.impl

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.shared.model.StreamType
import com.nikkiw.videofeedlab.shared.model.VideoItem
import kotlin.math.max
import kotlin.math.min

internal class AndroidPlaybackCoordinator(
    context: Context,
    private val items: List<VideoItem>,
    private val onDebugState: (PlaybackDebugState) -> Unit,
) {
    var isFirstFrameRendered by mutableStateOf(false)
        private set

    private val preloadStatusControl = FeedTargetPreloadStatusControl()
    private val preloadManagerBuilder =
        DefaultPreloadManager.Builder(
            context.applicationContext,
            preloadStatusControl,
        )
    private val preloadManager: DefaultPreloadManager = preloadManagerBuilder.build()

    val player: ExoPlayer =
        preloadManagerBuilder.buildExoPlayer().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }

    private val mediaItems: List<MediaItem> = items.map(::toMediaItem)
    private val addedToPreloadManager = mutableSetOf<Int>()

    private var currentIndex: Int = -1
    private var playbackRequestedAtMs: Long? = null
    private var firstFrameRendered = false
    private var rebufferCount = 0
    private var debugState = PlaybackDebugState()

    // Homing positions map for scrolling back and resuming
    private val savedPositions = mutableMapOf<String, Long>()

    private val playerListener =
        object : Player.Listener {
            override fun onRenderedFirstFrame() {
                if (firstFrameRendered) return
                firstFrameRendered = true
                isFirstFrameRendered = true
                val requestedAt = playbackRequestedAtMs ?: return
                debugState =
                    debugState.copy(
                        startupTimeMs = SystemClock.elapsedRealtime() - requestedAt,
                    )
                updateFormat()
                emitDebugState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING && firstFrameRendered) {
                    rebufferCount += 1
                    debugState = debugState.copy(rebufferCount = rebufferCount)
                    emitDebugState()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                debugState = debugState.copy(isPlaying = isPlaying)
                updateFormat()
                emitDebugState()
            }
        }

    init {
        player.addListener(playerListener)
        syncPreloadWindow(centerIndex = 0)
    }

    fun play(index: Int) {
        if (index !in mediaItems.indices || index == currentIndex) return

        // Save position of current video before switching
        if (currentIndex in mediaItems.indices) {
            val oldItem = items[currentIndex]
            savedPositions[oldItem.id] = player.currentPosition
        }

        currentIndex = index
        firstFrameRendered = false
        isFirstFrameRendered = false
        rebufferCount = 0
        playbackRequestedAtMs = SystemClock.elapsedRealtime()
        debugState = PlaybackDebugState(videoId = items[index].id)
        emitDebugState()

        syncPreloadWindow(centerIndex = index)

        val mediaItem = mediaItems[index]
        val preloadedSource = preloadManager.getMediaSource(mediaItem)
        if (preloadedSource != null) {
            player.setMediaSource(preloadedSource)
        } else {
            player.setMediaItem(mediaItem)
        }

        // Seek to saved position if it exists, otherwise seek to 0
        val savedPos = savedPositions[items[index].id]
        if (savedPos != null) {
            player.seekTo(savedPos)
        } else {
            player.seekTo(0)
        }

        player.prepare()
        player.playWhenReady = true
        preloadStatusControl.currentPlayingIndex = index
        preloadManager.setCurrentPlayingIndex(index)
        preloadManager.invalidate()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun setMuted(muted: Boolean) {
        player.volume = if (muted) 0f else 1f
    }

    fun release() {
        if (currentIndex in mediaItems.indices) {
            savedPositions[items[currentIndex].id] = player.currentPosition
        }
        player.removeListener(playerListener)
        player.release()
        preloadManager.release()
    }

    private fun syncPreloadWindow(centerIndex: Int) {
        val start = max(0, centerIndex - PRELOAD_WINDOW_RADIUS)
        val end = min(mediaItems.lastIndex, centerIndex + PRELOAD_WINDOW_RADIUS)
        val desired = (start..end).toSet()

        (addedToPreloadManager - desired).forEach { index ->
            preloadManager.remove(mediaItems[index])
            addedToPreloadManager.remove(index)
        }

        (desired - addedToPreloadManager).forEach { index ->
            preloadManager.add(mediaItems[index], index)
            addedToPreloadManager.add(index)
        }

        preloadManager.invalidate()
    }

    private fun updateFormat() {
        val format = player.videoFormat ?: return
        debugState =
            debugState.copy(
                bitrateKbps = format.bitrate.takeIf { it > 0 }?.div(1_000),
                resolution =
                    if (format.width > 0 && format.height > 0) {
                        "${format.width}×${format.height}"
                    } else {
                        null
                    },
            )
    }

    private fun emitDebugState() {
        onDebugState(debugState)
    }

    private fun toMediaItem(item: VideoItem): MediaItem {
        val mimeType =
            when (item.source.streamType) {
                StreamType.HLS -> MimeTypes.APPLICATION_M3U8
                StreamType.DASH -> MimeTypes.APPLICATION_MPD
                StreamType.PROGRESSIVE -> MimeTypes.VIDEO_MP4
            }
        return MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(item.source.uri)
            .setMimeType(mimeType)
            .build()
    }

    private companion object {
        const val PRELOAD_WINDOW_RADIUS = 4
    }
}
