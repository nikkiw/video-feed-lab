package com.nikkiw.videofeedlab.feature.videofeed.impl

import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/**
 * Attaches a [MediaPlayerEventAdapter] to the given [slot] and handles all
 * LibVLC playback lifecycle events on behalf of the coordinator.
 *
 * Extracted from [DesktopPlaybackCoordinator] to keep the class within the
 * Detekt [LargeClass] threshold.
 */
internal fun DesktopPlaybackCoordinator.addPlayerListener(slot: DesktopPlayerSlot) {
    val listener =
        object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                val assignment = validAssignment(slot, mediaPlayer) ?: return
                slot.phase = DesktopSlotPhase.PLAYING
                trace(slot, "playing")
                val seekMs = slot.pendingSeekMs
                slot.pendingSeekMs = 0L
                if (seekMs > 0L) {
                    mediaPlayer.submit { mediaPlayer.controls().setTime(seekMs) }
                }
                publish(assignment) {
                    copy(isPlaying = slot === activeSlot, errorMessage = null)
                }
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                val assignment =
                    validAssignment(slot, mediaPlayer)
                        ?: return
                val warmupPause = slot.consumeWarmupPause()

                if (slot !== activeSlot) {
                    if (warmupPause.wasRequested) {
                        slot.phase = DesktopSlotPhase.READY
                        trace(slot, "paused-ready")
                        markFrameReady(
                            slot = slot,
                            assignment = assignment,
                        )
                    } else if (slot.frameReady) {
                        // A previously active, presented slot is also reusable.
                        slot.phase = DesktopSlotPhase.READY
                        trace(slot, "paused-reusable")
                    } else {
                        trace(slot, "paused-ignored", "reason=no-playback-progress")
                    }
                } else {
                    if (warmupPause.resumeActive) {
                        slot.phase = DesktopSlotPhase.PREPARING
                        trace(slot, "paused-during-promotion", "action=resume")
                        publish(assignment) {
                            copy(
                                isPlaying = false,
                                isBuffering = true,
                                standbyReady = false,
                            )
                        }
                        mediaPlayer.submit {
                            if (
                                !released &&
                                slot.assignment == assignment &&
                                slot === activeSlot
                            ) {
                                mediaPlayer.controls().play()
                            }
                        }
                        return
                    }
                    slot.phase = DesktopSlotPhase.READY
                    trace(slot, "paused-active")
                    publish(assignment) {
                        copy(
                            isPlaying = false,
                            isBuffering = false,
                            standbyReady = false,
                        )
                    }
                }
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                val assignment = validAssignment(slot, mediaPlayer) ?: return
                // Replacing media can emit STOPPED for the previous MRL after the
                // new assignment has entered PREPARING. It must not clear it.
                if (slot.phase == DesktopSlotPhase.PREPARING) {
                    trace(slot, "stopped-ignored", "reason=replacement")
                    return
                }
                trace(slot, "stopped")
                if (slot.phase != DesktopSlotPhase.RELEASED) slot.phase = DesktopSlotPhase.EMPTY
                publish(assignment) { copy(isPlaying = false, isBuffering = false) }
            }

            override fun timeChanged(
                mediaPlayer: MediaPlayer,
                newTime: Long,
            ) {
                val assignment = validAssignment(slot, mediaPlayer) ?: return
                val isCurrentPlayback =
                    slot === activeSlot &&
                        assignment.index == currentIndex
                if (!isCurrentPlayback) {
                    if (
                        slot.phase == DesktopSlotPhase.PLAYING &&
                        shouldPauseStandbyAfterProgress(
                            pauseAfterWarmup = slot.shouldPauseAfterWarmup(),
                            playbackTimeMs = newTime,
                        ) &&
                        slot.requestWarmupPause()
                    ) {
                        trace(slot, "warmup-progress", "timeMs=$newTime action=pause")
                        mediaPlayer.submit {
                            val shouldPause =
                                !released &&
                                    slot.assignment == assignment &&
                                    slot.isWarmupPauseRequested() &&
                                    slot !== activeSlot
                            if (shouldPause) {
                                val pauseStartedAtNanos = System.nanoTime()
                                trace(slot, "pause-call-start", "reason=standby-warmup")
                                mediaPlayer.controls().pause()
                                trace(
                                    slot,
                                    "pause-call-return",
                                    "reason=standby-warmup durationMs=" +
                                        (System.nanoTime() - pauseStartedAtNanos) /
                                        NANOS_PER_MILLISECOND,
                                )
                            }
                        }
                    }
                    return
                }
                if (newTime > 0L) savedPositionsMs[assignment.videoId] = newTime
                if (!slot.frameReady) {
                    trace(slot, "active-startup-time", "timeMs=$newTime")
                }
                if (slot.buffering) {
                    slot.buffering = false
                    publish(assignment) {
                        copy(isBuffering = false)
                    }
                }
                if (
                    !slot.frameReady &&
                    hasAdvancedForPresentation(
                        playbackTimeMs = newTime,
                        baselineTimeMs = slot.presentationBaselineMs,
                    )
                ) {
                    markFrameReady(
                        slot = slot,
                        assignment = assignment,
                    )
                }
            }

            override fun buffering(
                mediaPlayer: MediaPlayer,
                newCache: Float,
            ) {
                val assignment = validAssignment(slot, mediaPlayer) ?: return
                val nowBuffering = newCache < BUFFER_COMPLETE_PERCENT
                val enteredRebuffer =
                    slot === activeSlot && nowBuffering && !slot.buffering && slot.frameReady
                slot.buffering = nowBuffering
                val shouldTraceBuffering =
                    slot === activeSlot &&
                        (newCache == 0f || newCache >= BUFFER_COMPLETE_PERCENT || enteredRebuffer)
                if (shouldTraceBuffering) {
                    trace(slot, "buffering", "cache=$newCache active=$nowBuffering")
                }
                publish(assignment) {
                    copy(
                        isBuffering = slot === activeSlot && nowBuffering,
                        rebufferCount = rebufferCount + if (enteredRebuffer) 1 else 0,
                    )
                }
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                val assignment = validAssignment(slot, mediaPlayer) ?: return
                slot.phase = DesktopSlotPhase.ENDED
                trace(slot, "finished")
                if (slot === activeSlot && assignment.index == currentIndex) {
                    slot.buffering = true
                    slot.presentationBaselineMs = 0L
                    slot.startupStartedAtNanos = System.nanoTime()
                    publish(assignment) {
                        copy(
                            isPlaying = false,
                            isBuffering = true,
                        )
                    }
                    mediaPlayer.submit {
                        mediaPlayer.controls().setTime(0L)
                        mediaPlayer.controls().play()
                    }
                }
            }

            override fun error(mediaPlayer: MediaPlayer) {
                val assignment = validAssignment(slot, mediaPlayer) ?: return
                slot.phase = DesktopSlotPhase.FAILED
                slot.buffering = false
                publish(assignment) {
                    copy(
                        isPlaying = false,
                        isBuffering = false,
                        standbyReady = false,
                        errorCount = errorCount + 1,
                        errorMessage = "LibVLC could not play this media source",
                    )
                }
            }
        }
    slot.listener = listener
    slot.player.events().addMediaPlayerEventListener(listener)
}

internal fun DesktopPlaybackCoordinator.markFrameReady(
    slot: DesktopPlayerSlot,
    assignment: DesktopSlotAssignment,
) {
    if (slot.frameReady) return

    val nowNanos = System.nanoTime()
    val isActive =
        slot === activeSlot &&
            assignment.index == currentIndex

    val startupMs =
        slot.startupStartedAtNanos
            .takeIf { isActive && it != 0L }
            ?.let { startedAt ->
                (nowNanos - startedAt) / NANOS_PER_MILLISECOND
            }

    val preloadMs =
        slot.preloadStartedAtNanos
            .takeIf { !isActive && it != 0L }
            ?.let { startedAt ->
                (nowNanos - startedAt) / NANOS_PER_MILLISECOND
            }

    slot.frameReady = true
    slot.buffering = false
    slot.startupStartedAtNanos = 0L
    slot.preloadStartedAtNanos = 0L
    trace(
        slot,
        "frame-ready",
        "active=$isActive startupMs=$startupMs preloadMs=$preloadMs",
    )

    publish(assignment) {
        copy(
            frameReady = true,
            isBuffering = false,
            standbyReady = !isActive,
            startupTimeMs =
                if (isActive) {
                    startupMs
                } else {
                    startupTimeMs
                },
            preloadTimeMs =
                if (isActive) {
                    preloadTimeMs
                } else {
                    preloadMs
                },
        )
    }
}
