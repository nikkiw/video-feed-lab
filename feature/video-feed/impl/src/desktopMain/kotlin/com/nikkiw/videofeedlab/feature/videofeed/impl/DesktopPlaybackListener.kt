package com.nikkiw.videofeedlab.feature.videofeed.impl

import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

/**
 * Replaces the slot listener for one media assignment and forwards native
 * callbacks to the coordinator's serial event loop. The listener captures an
 * immutable generation so in-flight callbacks from the previous assignment are
 * rejected after a hot-swap.
 */
internal fun DesktopPlaybackCoordinator.replacePlayerListener(
    slot: DesktopPlayerSlot,
    eventGeneration: Long,
) {
    slot.listener?.let { previous ->
        slot.player.events().removeMediaPlayerEventListener(previous)
    }
    val listener =
        object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                enqueuePlayerEvent(slot, mediaPlayer, eventGeneration) { assignment ->
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
            }

            override fun paused(mediaPlayer: MediaPlayer) {
                enqueuePlayerEvent(slot, mediaPlayer, eventGeneration) { assignment ->
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
                                if (isCurrentActiveAssignment(slot, assignment)) {
                                    mediaPlayer.controls().play()
                                }
                            }
                            return@enqueuePlayerEvent
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
            }

            override fun stopped(mediaPlayer: MediaPlayer) {
                enqueuePlayerEvent(slot, mediaPlayer, eventGeneration) { assignment ->
                    // Replacing media can emit STOPPED for the previous MRL after the
                    // new assignment has entered PREPARING. It must not clear it.
                    if (slot.phase == DesktopSlotPhase.PREPARING) {
                        trace(slot, "stopped-ignored", "reason=replacement")
                        return@enqueuePlayerEvent
                    }
                    trace(slot, "stopped")
                    if (slot.phase != DesktopSlotPhase.RELEASED) {
                        slot.phase = DesktopSlotPhase.EMPTY
                    }
                    publish(assignment) { copy(isPlaying = false, isBuffering = false) }
                }
            }

            override fun timeChanged(
                mediaPlayer: MediaPlayer,
                newTime: Long,
            ) {
                enqueuePlayerEvent(slot, mediaPlayer, eventGeneration) { assignment ->
                    val isCurrentPlayback =
                        slot === activeSlot &&
                            assignment.index == currentIndex
                    if (!isCurrentPlayback) {
                        handleStandbyTimeChanged(slot, mediaPlayer, assignment, newTime)
                        return@enqueuePlayerEvent
                    }
                    handleActiveTimeChanged(slot, assignment, newTime)
                }
            }

            override fun buffering(
                mediaPlayer: MediaPlayer,
                newCache: Float,
            ) {
                enqueuePlayerEvent(slot, mediaPlayer, eventGeneration) { assignment ->
                    val nowBuffering = newCache < BUFFER_COMPLETE_PERCENT
                    val enteredRebuffer =
                        slot === activeSlot && nowBuffering && !slot.buffering && slot.frameReady
                    slot.buffering = nowBuffering
                    if (enteredRebuffer) {
                        activePreloadEligible = false
                        slot.preloadEligibilityBaselineMs =
                            runCatching { mediaPlayer.status().time() }.getOrDefault(0L)
                        suspendStandbyWarmupForActiveRebuffer()
                    }
                    val shouldTraceBuffering =
                        slot === activeSlot &&
                            (
                                newCache == 0f ||
                                    newCache >= BUFFER_COMPLETE_PERCENT ||
                                    enteredRebuffer
                            )
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
            }

            override fun finished(mediaPlayer: MediaPlayer) {
                enqueuePlayerEvent(slot, mediaPlayer, eventGeneration) {
                    // :input-repeat owns looping. A second seek/play path here races
                    // with LibVLC repeat and can cause a visible restart or duplicate play.
                    trace(slot, "finished", "loop-owner=native")
                }
            }

            override fun error(mediaPlayer: MediaPlayer) {
                enqueuePlayerEvent(slot, mediaPlayer, eventGeneration) { assignment ->
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
        }
    slot.listener = listener
    slot.player.events().addMediaPlayerEventListener(listener)
}

private fun DesktopPlaybackCoordinator.handleStandbyTimeChanged(
    slot: DesktopPlayerSlot,
    mediaPlayer: MediaPlayer,
    assignment: DesktopSlotAssignment,
    newTime: Long,
) {
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
                isCurrentAssignment(slot, assignment) &&
                    slot.isWarmupPauseRequested() &&
                    activeSlotId != slot.id
            if (shouldPause) {
                val pauseStartedAtNanos = System.nanoTime()
                traceFromPlayerThread(
                    slot = slot,
                    event = "pause-call-start",
                    details = "reason=standby-warmup",
                )
                mediaPlayer.controls().pause()
                traceFromPlayerThread(
                    slot = slot,
                    event = "pause-call-return",
                    details =
                        "reason=standby-warmup durationMs=" +
                            (System.nanoTime() - pauseStartedAtNanos) /
                            NANOS_PER_MILLISECOND,
                )
            }
        }
    }
}

private fun DesktopPlaybackCoordinator.handleActiveTimeChanged(
    slot: DesktopPlayerSlot,
    assignment: DesktopSlotAssignment,
    newTime: Long,
) {
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
    if (
        !activePreloadEligible &&
        canStartStandbyPreload(
            sourceMode = config.sourceMode,
            frameReady = slot.frameReady,
            isBuffering = slot.buffering,
            playbackTimeMs = newTime,
            baselineTimeMs = slot.preloadEligibilityBaselineMs,
            minimumStablePlaybackMs = config.activeStablePlaybackMs,
        )
    ) {
        activePreloadEligible = true
        trace(
            slot,
            "active-stable",
            "timeMs=$newTime baselineMs=${slot.preloadEligibilityBaselineMs}",
        )
        startPendingPreloadIfEligible()
    }
}

private fun DesktopPlaybackCoordinator.enqueuePlayerEvent(
    slot: DesktopPlayerSlot,
    mediaPlayer: MediaPlayer,
    eventGeneration: Long,
    handler: (DesktopSlotAssignment) -> Unit,
) {
    dispatchCallback {
        val assignment =
            validAssignment(
                slot = slot,
                mediaPlayer = mediaPlayer,
                eventGeneration = eventGeneration,
            ) ?: return@dispatchCallback
        handler(assignment)
    }
}

internal fun DesktopPlaybackCoordinator.markFrameReady(
    slot: DesktopPlayerSlot,
    assignment: DesktopSlotAssignment,
) {
    assertEventLoopThread()
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
