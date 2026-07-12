package com.nikkiw.videofeedlab.feature.videofeed.impl

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.feature.videofeed.impl.cache.CacheManager
import com.nikkiw.videofeedlab.shared.model.StreamType
import com.nikkiw.videofeedlab.shared.model.VideoItem
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
internal class AndroidPlaybackCoordinator(
    context: Context,
    private val items: List<VideoItem>,
    private val onDebugState: (PlaybackDebugState) -> Unit,
) {
    val isLowRamDevice: Boolean
    private val cacheDataSourceFactory = CacheManager.getCacheDataSourceFactory(context)

    // Players pool
    private val activePlayer: ExoPlayer
    private val preloadPlayer: ExoPlayer? // null on Low-RAM devices

    // State mapping
    private val pageToPlayerMap = mutableStateMapOf<Int, ExoPlayer>()
    private val firstFrameRenderedMap = mutableStateMapOf<Int, Boolean>()

    private val preloadStatusControl: FeedTargetPreloadStatusControl
    private val preloadManager: DefaultPreloadManager

    private val mediaItems: List<MediaItem> = items.map(::toMediaItem)
    private var currentIndex: Int = -1
    private var playbackRequestedAtMs: Long? = null
    private var rebufferCount = 0
    private var debugState = PlaybackDebugState()
    private var isMuted = false

    private val savedPositions = mutableMapOf<String, Long>()

    init {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        isLowRamDevice = activityManager.isLowRamDevice
        AnalyticsManager.isLowRamDevice = isLowRamDevice

        preloadStatusControl = FeedTargetPreloadStatusControl(isLowRamDevice)

        val preloadManagerBuilder =
            DefaultPreloadManager.Builder(context.applicationContext, preloadStatusControl)
                .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))

        preloadManager = preloadManagerBuilder.build()

        activePlayer =
            preloadManagerBuilder.buildExoPlayer().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }

        preloadPlayer =
            if (!isLowRamDevice) {
                preloadManagerBuilder.buildExoPlayer().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0f
                }
            } else {
                null
            }

        setupListeners()
        play(0)
    }

    private fun setupListeners() {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    val player = playerPool().firstOrNull { it.volume > 0f || it.playWhenReady } ?: activePlayer
                    val index = pageToPlayerMap.entries.firstOrNull { it.value == player }?.key ?: return
                    firstFrameRenderedMap[index] = true

                    if (index == currentIndex) {
                        val requestedAt = playbackRequestedAtMs ?: return
                        val latency = SystemClock.elapsedRealtime() - requestedAt
                        AnalyticsManager.trackStartupTime(items[index].id, latency)

                        debugState = debugState.copy(startupTimeMs = latency)
                        updateFormat(player)
                        emitDebugState()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val player = playerPool().firstOrNull { it.playWhenReady } ?: activePlayer
                    val index = pageToPlayerMap.entries.firstOrNull { it.value == player }?.key ?: return
                    if (index == currentIndex) {
                        if (playbackState == Player.STATE_BUFFERING) {
                            rebufferCount += 1
                            AnalyticsManager.trackRebuffer(items[index].id)
                            debugState = debugState.copy(rebufferCount = rebufferCount)
                            emitDebugState()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        val index = pageToPlayerMap.entries.firstOrNull { it.value == activePlayer }?.key ?: return
                        AnalyticsManager.trackVideoStart(items[index].id)
                    }
                    debugState = debugState.copy(isPlaying = isPlaying)
                    emitDebugState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    val player = playerPool().firstOrNull { it == activePlayer } ?: activePlayer
                    val index = pageToPlayerMap.entries.firstOrNull { it.value == player }?.key ?: return
                    AnalyticsManager.trackError(items[index].id, error.message ?: "Unknown ExoPlayer Error")
                }
            }

        activePlayer.addListener(listener)
        preloadPlayer?.addListener(listener)
    }

    private fun playerPool() = listOfNotNull(activePlayer, preloadPlayer)

    fun getPlayerForIndex(index: Int): ExoPlayer? = pageToPlayerMap[index]

    fun isFirstFrameRenderedForIndex(index: Int): Boolean = firstFrameRenderedMap[index] ?: false

    fun play(index: Int) {
        if (index !in items.indices) return

        if (currentIndex != -1 && currentIndex != index) {
            println("[EnterpriseAnalytics] 📊 Event: PAGE_SCROLL | From: $currentIndex | To: $index")
        }

        if (isLowRamDevice) {
            playSinglePlayer(index)
        } else {
            playMultiPlayer(index)
        }
    }

    private fun playSinglePlayer(index: Int) {
        if (currentIndex in items.indices) {
            savedPositions[items[currentIndex].id] = activePlayer.currentPosition
        }

        currentIndex = index
        firstFrameRenderedMap[index] = false
        pageToPlayerMap.clear()
        pageToPlayerMap[index] = activePlayer

        activePlayer.stop()
        val mediaItem = mediaItems[index]
        val preloadedSource = preloadManager.getMediaSource(mediaItem)
        if (preloadedSource != null) {
            activePlayer.setMediaSource(preloadedSource)
        } else {
            activePlayer.setMediaItem(mediaItem)
        }

        val savedPos = savedPositions[items[index].id]
        activePlayer.seekTo(savedPos ?: 0)
        activePlayer.prepare()
        activePlayer.volume = if (isMuted) 0f else 1f
        activePlayer.playWhenReady = true

        playbackRequestedAtMs = SystemClock.elapsedRealtime()
        rebufferCount = 0
        debugState = PlaybackDebugState(videoId = items[index].id)
        emitDebugState()

        syncPreloadWindow(index)
    }

    private fun playMultiPlayer(index: Int) {
        val oldActiveIndex = currentIndex
        currentIndex = index

        if (oldActiveIndex != -1 && oldActiveIndex != index) {
            pageToPlayerMap[oldActiveIndex]?.let { player ->
                savedPositions[items[oldActiveIndex].id] = player.currentPosition
                player.stop()
            }
        }

        pageToPlayerMap.clear()

        // Active Player setup
        pageToPlayerMap[index] = activePlayer
        activePlayer.stop()
        val mediaItem = mediaItems[index]
        val preloadedSource = preloadManager.getMediaSource(mediaItem)
        if (preloadedSource != null) {
            activePlayer.setMediaSource(preloadedSource)
        } else {
            activePlayer.setMediaItem(mediaItem)
        }
        activePlayer.seekTo(savedPositions[items[index].id] ?: 0)
        activePlayer.prepare()
        activePlayer.volume = if (isMuted) 0f else 1f
        activePlayer.playWhenReady = true

        playbackRequestedAtMs = SystemClock.elapsedRealtime()
        rebufferCount = 0
        debugState = PlaybackDebugState(videoId = items[index].id)
        emitDebugState()

        // Preload Player setup
        val nextIndex = index + 1
        if (nextIndex in items.indices && preloadPlayer != null) {
            pageToPlayerMap[nextIndex] = preloadPlayer
            preloadPlayer.stop()
            val nextMediaItem = mediaItems[nextIndex]
            val nextPreloadedSource = preloadManager.getMediaSource(nextMediaItem)
            if (nextPreloadedSource != null) {
                preloadPlayer.setMediaSource(nextPreloadedSource)
            } else {
                preloadPlayer.setMediaItem(nextMediaItem)
            }
            preloadPlayer.seekTo(savedPositions[items[nextIndex].id] ?: 0)
            preloadPlayer.prepare()
            preloadPlayer.volume = 0f
            preloadPlayer.playWhenReady = false
            firstFrameRenderedMap[nextIndex] = false
        }

        syncPreloadWindow(index)
    }

    fun togglePlayPause() {
        activePlayer.playWhenReady = !activePlayer.playWhenReady
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        activePlayer.volume = if (muted) 0f else 1f
    }

    fun release() {
        if (currentIndex in items.indices) {
            savedPositions[items[currentIndex].id] = activePlayer.currentPosition
        }
        activePlayer.release()
        preloadPlayer?.release()
        pageToPlayerMap.clear()
        firstFrameRenderedMap.clear()
        preloadManager.release()
    }

    private fun syncPreloadWindow(centerIndex: Int) {
        preloadManager.setCurrentPlayingIndex(centerIndex)
        val start = max(0, centerIndex - 2)
        val end = min(mediaItems.lastIndex, centerIndex + 2)
        for (i in start..end) {
            preloadManager.add(mediaItems[i], i)
        }
        preloadManager.invalidate()
    }

    private fun updateFormat(player: ExoPlayer) {
        val format = player.videoFormat ?: return
        debugState =
            debugState.copy(
                bitrateKbps = format.bitrate.takeIf { it > 0 }?.div(1_000),
                resolution = if (format.width > 0 && format.height > 0) "${format.width}×${format.height}" else null,
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
}
