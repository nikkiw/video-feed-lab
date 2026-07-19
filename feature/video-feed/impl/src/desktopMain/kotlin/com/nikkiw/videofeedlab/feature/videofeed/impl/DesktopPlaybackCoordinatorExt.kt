package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.nikkiw.videofeedlab.shared.model.thumbnailUrlAt
import kotlinx.coroutines.flow.update

/**
 * Private-scope coordinator operations extracted to keep [DesktopPlaybackCoordinator]
 * within the Detekt [LargeClass] threshold.
 *
 * Validates that [mediaPlayer] still belongs to [slot]'s current assignment.
 * Returns null if release has started, the per-assignment listener generation is
 * stale, the player doesn't match, or the MRL changed during a media hot-swap.
 */
internal fun DesktopPlaybackCoordinator.validAssignment(
    slot: DesktopPlayerSlot,
    mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer,
    eventGeneration: Long,
): DesktopSlotAssignment? {
    assertEventLoopThread()
    val current = slot.assignment
    val isValid =
        !released &&
            slot.player === mediaPlayer &&
            current != null &&
            matchesGeneration(current, eventGeneration) &&
            isCurrentAssignment(slot, current)

    if (!isValid) return null

    val currentMrl = runCatching { mediaPlayer.media().info().mrl() }.getOrNull()
    return current?.takeIf { currentMrl == null || currentMrl == it.sourceUri }
}

internal fun DesktopPlaybackCoordinator.assign(
    slot: DesktopPlayerSlot,
    index: Int,
    startPaused: Boolean,
) {
    assertEventLoopThread()
    val previousAssignment = slot.assignment
    if (previousAssignment?.index == currentIndex && slot === activeSlot) {
        saveActivePosition()
    }
    previousAssignment?.let { old ->
        updatePage(old.index) {
            copy(
                surfaceId = null,
                posterUrl = posterUrlFor(old.index),
                frameReady = false,
                isPlaying = false,
                isBuffering = false,
                standbyReady = false,
            )
        }
    }

    val assignment =
        DesktopSlotAssignment(
            index = index,
            videoId = items[index].id,
            sourceUri = items[index].source.uri,
            generation = ++generation,
        )
    slot.assignment = assignment
    replacePlayerListener(slot, assignment.generation)
    slot.phase = DesktopSlotPhase.PREPARING
    slot.buffering = true

    val startedAtNanos = System.nanoTime()

    slot.startupStartedAtNanos =
        if (startPaused) {
            0L
        } else {
            startedAtNanos
        }

    slot.preloadStartedAtNanos =
        if (startPaused) {
            startedAtNanos
        } else {
            0L
        }

    slot.pendingSeekMs = positionFor(index)
    slot.presentationBaselineMs = slot.pendingSeekMs
    slot.frameReady = false
    slot.resetWarmup(startPaused)
    trace(slot, "assign", "mode=${if (startPaused) "standby" else "active"}")

    updatePage(index) {
        copy(
            surfaceId = slot.id,
            posterUrl = posterUrlFor(index),
            frameReady = false,
            isPlaying = false,
            isBuffering = true,
            standbyReady = false,
            errorMessage = null,
            startupSource =
                if (startPaused) {
                    DesktopStartupSource.STANDBY
                } else {
                    DesktopStartupSource.COLD
                },
            preloadTimeMs = null,
        )
    }

    val options = config.mediaOptions(isStandby = startPaused)
    slot.player.audio().setMute(startPaused || muted)
    /*
     * LibVLC's startPaused() can emit paused before a DASH stream has decoded a
     * frame. Play muted until the playback clock advances, then pause from the
     * timeChanged callback so READY means that the standby really warmed up.
     */
    slot.execute(
        PendingPlayerAction.PlayMrl(
            mrl = assignment.sourceUri,
            generation = assignment.generation,
            options = options,
        ),
    )
}

internal fun DesktopPlaybackCoordinator.promoteReadyStandby(
    slot: DesktopPlayerSlot,
    index: Int,
) {
    slot.clearWarmup()
    slot.startupStartedAtNanos = 0L
    slot.preloadStartedAtNanos = 0L

    updatePage(index) {
        copy(
            frameReady = true,
            isBuffering = false,
            standbyReady = false,
            errorMessage = null,
            startupSource = DesktopStartupSource.STANDBY,
            startupTimeMs = 0L,
        )
    }

    slot.player.audio().setMute(muted)
    slot.executeResume()
}

internal fun DesktopPlaybackCoordinator.promoteWarmingStandby(
    slot: DesktopPlayerSlot,
    index: Int,
) {
    val awaitingWarmupPause = slot.promoteWarmupToActive()
    slot.startupStartedAtNanos = System.nanoTime()

    updatePage(index) {
        copy(
            isBuffering = !slot.frameReady,
            standbyReady = false,
            errorMessage = null,
            startupSource = DesktopStartupSource.STANDBY,
        )
    }

    slot.player.audio().setMute(muted)
    if (!awaitingWarmupPause) {
        slot.executeResume()
    }
}

internal fun DesktopPlaybackCoordinator.prepareAdjacent(
    centerIndex: Int,
    direction: DesktopScrollDirection,
) {
    preferredAdjacentIndex(centerIndex, direction, items.lastIndex)?.let(::preloadPageOnEventLoop)
}

internal fun DesktopPlaybackCoordinator.saveActivePosition() {
    val slot = activeSlot ?: return
    val assignment = slot.assignment ?: return
    val position = runCatching { slot.player.status().time() }.getOrDefault(0L)
    if (position > 0L) savedPositionsMs[assignment.videoId] = position
    updatePage(assignment.index) {
        copy(posterUrl = items[assignment.index].thumbnailUrlAt(position))
    }
}

internal fun DesktopPlaybackCoordinator.positionFor(index: Int): Long = savedPositionsMs[items[index].id] ?: 0L

internal fun DesktopPlaybackCoordinator.posterUrlFor(index: Int): String {
    val positionMs = positionFor(index)
    val images = items[index].images
    return if (positionMs > 0L) {
        items[index].thumbnailUrlAt(positionMs)
    } else {
        images.posterUrl
    }
}

internal fun DesktopPlaybackCoordinator.slotAssignedTo(index: Int): DesktopPlayerSlot? {
    return slots.firstOrNull { it.assignment?.index == index }
}

internal fun DesktopPlaybackCoordinator.slotForNewAssignment(): DesktopPlayerSlot =
    slots.firstOrNull { it !== activeSlot } ?: requireNotNull(activeSlot)

internal fun DesktopPlaybackCoordinator.updatePage(
    index: Int,
    transform: DesktopPagePlaybackState.() -> DesktopPagePlaybackState,
) {
    assertEventLoopThread()
    mutableState.update { state ->
        val page = state.pages[index] ?: return@update state
        state.copy(pages = state.pages + (index to page.transform()))
    }
}

internal fun DesktopPlaybackCoordinator.publish(
    assignment: DesktopSlotAssignment,
    transform: DesktopPagePlaybackState.() -> DesktopPagePlaybackState,
) {
    assertEventLoopThread()
    if (released || slotAssignedTo(assignment.index)?.assignment != assignment) return
    updatePage(assignment.index, transform)
}

internal fun DesktopPlaybackCoordinator.trace(
    slot: DesktopPlayerSlot,
    event: String,
    details: String = "",
) {
    assertEventLoopThread()
    traceFromPlayerThread(slot, event, details)
}

internal fun traceFromPlayerThread(
    slot: DesktopPlayerSlot,
    event: String,
    details: String = "",
) {
    val assignment = slot.assignment
    val suffix = details.takeIf(String::isNotEmpty)?.let { " $it" }.orEmpty()
    DesktopPlaybackTrace.log {
        "[DesktopPlayback] time=${System.currentTimeMillis()} " +
            "slot=${slot.id} index=${assignment?.index} generation=${assignment?.generation} " +
            "phase=${slot.phase} event=$event$suffix"
    }
}
