package com.nikkiw.videofeedlab.feature.videofeed.impl

import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.Component

internal sealed interface PendingPlayerAction {
    object None : PendingPlayerAction

    data class PlayMrl(
        val mrl: String,
        val options: Array<out String>,
    ) : PendingPlayerAction {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PlayMrl
            if (mrl != other.mrl) return false
            if (!options.contentEquals(other.options)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = mrl.hashCode()
            result = 31 * result + options.contentHashCode()
            return result
        }
    }

    object Resume : PendingPlayerAction
}

internal class DesktopPlayerSlot(
    val id: Int,
    val component: CallbackMediaPlayerComponent,
    val player: EmbeddedMediaPlayer,
    val surface: Component,
) {
    private val warmupLock = Any()
    private var pauseAfterWarmup = false
    private var warmupPauseRequested = false
    private var resumeAfterWarmupPause = false

    var assignment: DesktopSlotAssignment? = null
    var phase: DesktopSlotPhase = DesktopSlotPhase.EMPTY
    var pendingSeekMs: Long = 0L
    var presentationBaselineMs: Long = 0L
    var startupStartedAtNanos: Long = 0L
    var frameReady: Boolean = false
    var preloadStartedAtNanos: Long = 0L
    var buffering: Boolean = false
    var listener: MediaPlayerEventAdapter? = null
    var pendingAction: PendingPlayerAction = PendingPlayerAction.None

    fun execute(action: PendingPlayerAction) {
        if (surface.isDisplayable) {
            pendingAction = PendingPlayerAction.None
            when (action) {
                is PendingPlayerAction.PlayMrl -> {
                    player.submit {
                        val currentAssignment = assignment
                        if (currentAssignment?.sourceUri == action.mrl) {
                            val startedAtNanos =
                                if (DesktopPlaybackTrace.enabled) System.nanoTime() else 0L
                            DesktopPlaybackTrace.log {
                                "[DesktopPlayback] time=${System.currentTimeMillis()} " +
                                    "slot=$id index=${currentAssignment.index} " +
                                    "generation=${currentAssignment.generation} " +
                                    "phase=$phase event=media-play-start"
                            }
                            player.media().play(
                                action.mrl,
                                *action.options,
                            )
                            DesktopPlaybackTrace.log {
                                val durationMs =
                                    (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLISECOND
                                "[DesktopPlayback] time=${System.currentTimeMillis()} " +
                                    "slot=$id index=${currentAssignment.index} " +
                                    "generation=${currentAssignment.generation} " +
                                    "phase=$phase event=media-play-return durationMs=$durationMs"
                            }
                        }
                    }
                }

                is PendingPlayerAction.Resume -> {
                    player.controls().play()
                }

                PendingPlayerAction.None -> {}
            }
        } else {
            pendingAction = action
        }
    }

    fun executeResume() {
        val action = pendingAction
        if (action is PendingPlayerAction.PlayMrl) {
            // Already going to play the media when displayable, nothing to do
        } else {
            execute(PendingPlayerAction.Resume)
        }
    }

    fun resetWarmup(enabled: Boolean) {
        synchronized(warmupLock) {
            pauseAfterWarmup = enabled
            warmupPauseRequested = false
            resumeAfterWarmupPause = false
        }
    }

    fun clearWarmup() {
        resetWarmup(enabled = false)
    }

    fun shouldPauseAfterWarmup(): Boolean = synchronized(warmupLock) { pauseAfterWarmup }

    fun requestWarmupPause(): Boolean =
        synchronized(warmupLock) {
            if (!pauseAfterWarmup || warmupPauseRequested) {
                false
            } else {
                pauseAfterWarmup = false
                warmupPauseRequested = true
                true
            }
        }

    fun isWarmupPauseRequested(): Boolean = synchronized(warmupLock) { warmupPauseRequested }

    fun promoteWarmupToActive(): Boolean =
        synchronized(warmupLock) {
            pauseAfterWarmup = false
            if (warmupPauseRequested) {
                resumeAfterWarmupPause = true
                true
            } else {
                false
            }
        }

    fun consumeWarmupPause(): WarmupPauseOutcome =
        synchronized(warmupLock) {
            WarmupPauseOutcome(
                wasRequested = warmupPauseRequested,
                resumeActive = warmupPauseRequested && resumeAfterWarmupPause,
            ).also {
                warmupPauseRequested = false
                resumeAfterWarmupPause = false
            }
        }
}

internal data class DesktopSlotAssignment(
    val index: Int,
    val videoId: String,
    val sourceUri: String,
    val generation: Long,
)

internal data class WarmupPauseOutcome(
    val wasRequested: Boolean,
    val resumeActive: Boolean,
)

internal data class CapturedDesktopFrame(
    val index: Int,
    val frame: androidx.compose.ui.graphics.ImageBitmap,
)

internal fun DesktopPlayerSlot.capturePresentedFrame(): CapturedDesktopFrame? {
    val currentAssignment = assignment
    val frame =
        if (frameReady && currentAssignment != null) {
            captureComponentFrame(component.videoSurfaceComponent())
        } else {
            null
        }
    return if (currentAssignment != null && frame != null) {
        CapturedDesktopFrame(index = currentAssignment.index, frame = frame)
    } else {
        null
    }
}
