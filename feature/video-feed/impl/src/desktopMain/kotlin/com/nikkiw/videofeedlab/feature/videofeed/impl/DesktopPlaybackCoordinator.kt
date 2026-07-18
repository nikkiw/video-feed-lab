package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.nikkiw.videofeedlab.shared.model.VideoItem
import java.awt.Component
import java.awt.Dimension
import javax.swing.SwingUtilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent

/**
 * Screen-scoped desktop player that reuses one native LibVLC pipeline for the entire feed.
 */
internal class DesktopPlaybackCoordinator private constructor(
    private val items: List<VideoItem>,
    private val component: CallbackMediaPlayerComponent,
) {
    private val player = component.mediaPlayer()
    private val savedPositionsMs = mutableMapOf<String, Long>()
    private val mutableState = MutableStateFlow(DesktopPlaybackState())

    val state: StateFlow<DesktopPlaybackState> = mutableState.asStateFlow()
    val videoComponent: Component = component

    private var currentIndex = NO_ACTIVE_INDEX
    private var pendingSeekMs = 0L
    private var startupStartedAtNanos = 0L
    private var muted = false
    private var buffering = false
    private var released = false

    init {
        component.minimumSize = Dimension(1, 1)
        component.preferredSize = Dimension(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT)
        addPlayerListener()
    }

    fun play(index: Int) {
        if (released || index !in items.indices) return

        if (index == currentIndex) {
            player.audio().setMute(muted)
            player.controls().play()
            return
        }

        saveCurrentPosition()
        currentIndex = index
        pendingSeekMs = savedPositionsMs[items[index].id] ?: 0L
        startupStartedAtNanos = System.nanoTime()
        buffering = false

        mutableState.value =
            DesktopPlaybackState(
                activeIndex = index,
                videoId = items[index].id,
                isBuffering = true,
            )

        player.audio().setMute(muted)
        player.media().play(items[index].source.uri, *MEDIA_OPTIONS)
    }

    fun onScrollStart() {
        if (!released) {
            player.controls().pause()
        }
    }

    fun togglePlayPause() {
        if (released || currentIndex !in items.indices) return

        if (player.status().isPlaying) {
            player.controls().pause()
        } else {
            player.controls().play()
        }
    }

    fun setMuted(value: Boolean) {
        if (released) return
        muted = value
        player.audio().setMute(value)
    }

    fun release() {
        if (released) return
        released = true
        saveCurrentPosition()
        runCatching { player.controls().stop() }
        runCatching { component.release() }
    }

    private fun addPlayerListener() {
        player.events().addMediaPlayerEventListener(
            object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer) {
                    val seekMs = pendingSeekMs
                    pendingSeekMs = 0L
                    if (seekMs > 0L) {
                        mediaPlayer.submit { mediaPlayer.controls().setTime(seekMs) }
                    }
                    publish { copy(isPlaying = true, errorMessage = null) }
                }

                override fun paused(mediaPlayer: MediaPlayer) {
                    publish { copy(isPlaying = false) }
                }

                override fun stopped(mediaPlayer: MediaPlayer) {
                    publish { copy(isPlaying = false, isBuffering = false) }
                }

                override fun videoOutput(
                    mediaPlayer: MediaPlayer,
                    newCount: Int,
                ) {
                    if (newCount <= 0) return

                    val startupMs =
                        startupStartedAtNanos
                            .takeIf { it != 0L }
                            ?.let { (System.nanoTime() - it) / NANOS_PER_MILLISECOND }
                    startupStartedAtNanos = 0L
                    buffering = false
                    publish {
                        copy(
                            firstFrameRendered = true,
                            isBuffering = false,
                            startupTimeMs = startupMs,
                        )
                    }
                }

                override fun buffering(
                    mediaPlayer: MediaPlayer,
                    newCache: Float,
                ) {
                    val nowBuffering = newCache < BUFFER_COMPLETE_PERCENT
                    val enteredRebuffer =
                        nowBuffering && !buffering && mutableState.value.firstFrameRendered
                    buffering = nowBuffering
                    publish {
                        copy(
                            isBuffering = nowBuffering,
                            rebufferCount = rebufferCount + if (enteredRebuffer) 1 else 0,
                        )
                    }
                }

                override fun finished(mediaPlayer: MediaPlayer) {
                    if (released || currentIndex !in items.indices) return
                    mediaPlayer.submit {
                        mediaPlayer.controls().setTime(0L)
                        mediaPlayer.controls().play()
                    }
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    buffering = false
                    publish {
                        copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorCount = errorCount + 1,
                            errorMessage = "LibVLC could not play this media source",
                        )
                    }
                }
            },
        )
    }

    private fun saveCurrentPosition() {
        if (currentIndex !in items.indices) return
        val position = runCatching { player.status().time() }.getOrDefault(0L)
        if (position > 0L) {
            savedPositionsMs[items[currentIndex].id] = position
        }
    }

    private fun publish(transform: DesktopPlaybackState.() -> DesktopPlaybackState) {
        if (released) return
        // LibVLC callbacks arrive on native worker threads. Publish in one UI-thread order.
        SwingUtilities.invokeLater {
            if (!released) {
                mutableState.update(transform)
            }
        }
    }

    internal companion object {
        fun create(items: List<VideoItem>): Result<DesktopPlaybackCoordinator> =
            runCatching {
                DesktopPlaybackCoordinator(
                    items = items,
                    component = CallbackMediaPlayerComponent(),
                )
            }.recoverCatching { cause ->
                throw IllegalStateException(
                    "LibVLC was not found. Install VLC 3.x with the same CPU architecture as the JDK.",
                    cause,
                )
            }

        private const val NO_ACTIVE_INDEX = -1
        private const val DEFAULT_VIDEO_WIDTH = 540
        private const val DEFAULT_VIDEO_HEIGHT = 960
        private const val BUFFER_COMPLETE_PERCENT = 100f
        private const val NANOS_PER_MILLISECOND = 1_000_000L

        private val MEDIA_OPTIONS =
            arrayOf(
                ":network-caching=1200",
                ":live-caching=1200",
                ":file-caching=300",
                ":no-video-title-show",
            )
    }
}

internal data class DesktopPlaybackState(
    val activeIndex: Int = -1,
    val videoId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val firstFrameRendered: Boolean = false,
    val startupTimeMs: Long? = null,
    val rebufferCount: Int = 0,
    val errorCount: Int = 0,
    val errorMessage: String? = null,
)
