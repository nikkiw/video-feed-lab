package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.nikkiw.videofeedlab.shared.model.VideoItem
import com.nikkiw.videofeedlab.shared.model.thumbnailUrlAt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.Canvas
import java.awt.Component
import java.awt.Dimension
import java.awt.event.HierarchyListener
import javax.swing.SwingUtilities

/** Screen-scoped two-slot LibVLC pool backed by one native LibVLC instance. */
internal class DesktopPlaybackCoordinator private constructor(
    private val items: List<VideoItem>,
    private val factory: MediaPlayerFactory,
    private val slots: List<DesktopPlayerSlot>,
) {
    private val savedPositionsMs = mutableMapOf<String, Long>()
    private val mutableState =
        MutableStateFlow(
            DesktopPlaybackState(
                pages =
                    items.indices.associateWith { index ->
                        DesktopPagePlaybackState(posterUrl = items[index].images.posterUrl)
                    },
            ),
        )

    val state: StateFlow<DesktopPlaybackState> = mutableState.asStateFlow()

    private var activeSlot: DesktopPlayerSlot? = null
    private var currentIndex = NO_ACTIVE_INDEX
    private var generation = 0L
    private var muted = false
    private var released = false

    init {
        slots.forEach(::addPlayerListener)
    }

    fun videoComponent(surfaceId: Int): Component? = slots.getOrNull(surfaceId)?.surface

    fun play(index: Int) {
        if (released || index !in items.indices) return

        val current = activeSlot
        if (index == currentIndex && current?.assignment?.index == index) {
            current.player.audio().setMute(muted)
            current.executeResume()
            return
        }

        saveActivePosition()
        val previousIndex = currentIndex
        val direction = scrollDirection(previousIndex, index)
        val target = slotAssignedTo(index) ?: slotForNewAssignment()

        activeSlot?.takeUnless { it === target }?.let { old ->
            old.player.controls().pause()
            old.player.audio().setMute(true)
        }
        currentIndex = index
        activeSlot = target
        mutableState.update { it.copy(activeIndex = index) }

        val wasPrepared = target.assignment?.index == index && target.phase != DesktopSlotPhase.FAILED
        if (!wasPrepared) {
            assign(target, index, startPaused = false)
        } else {
            target.startupStartedAtNanos = System.nanoTime()
            updatePage(index) {
                copy(
                    isBuffering = !firstFramePresented,
                    errorMessage = null,
                    startupSource = DesktopStartupSource.STANDBY,
                )
            }
            target.player.audio().setMute(muted)
            target.executeResume()
        }

        prepareAdjacent(index, direction)
    }

    fun preloadPage(index: Int) {
        if (released || index !in items.indices || index == currentIndex) return
        val existing = slotAssignedTo(index)
        val alreadyPrepared =
            existing != null && existing !== activeSlot && existing.phase != DesktopSlotPhase.FAILED
        if (!alreadyPrepared) {
            slots.firstOrNull { it !== activeSlot }?.let { standby ->
                assign(standby, index, startPaused = true)
            }
        }
    }

    fun onScrollStart() {
        if (released) return
        saveActivePosition()
        activeSlot?.player?.controls()?.pause()
    }

    fun togglePlayPause() {
        if (released || currentIndex !in items.indices) return
        activeSlot?.player?.let { player ->
            if (player.status().isPlaying) player.controls().pause() else player.controls().play()
        }
    }

    fun setMuted(value: Boolean) {
        if (released) return
        muted = value
        slots.forEach { slot -> slot.player.audio().setMute(slot !== activeSlot || value) }
    }

    fun release() {
        if (released) return
        released = true
        saveActivePosition()
        slots.forEach { slot ->
            slot.listener?.let { listener ->
                runCatching { slot.player.events().removeMediaPlayerEventListener(listener) }
            }
            runCatching { slot.player.controls().stop() }
            runCatching { slot.component.release() }
            slot.assignment = null
            slot.phase = DesktopSlotPhase.RELEASED
        }
        runCatching { factory.release() }
    }

    private fun assign(
        slot: DesktopPlayerSlot,
        index: Int,
        startPaused: Boolean,
    ) {
        val previousAssignment = slot.assignment
        if (previousAssignment?.index == currentIndex && slot === activeSlot) {
            saveActivePosition()
        }
        runCatching { slot.player.controls().stop() }
        previousAssignment?.let { old ->
            updatePage(old.index) {
                copy(
                    surfaceId = null,
                    posterUrl = posterUrlFor(old.index),
                    firstFramePresented = false,
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
        slot.phase = DesktopSlotPhase.PREPARING
        slot.buffering = true
        slot.startupStartedAtNanos = if (startPaused) 0L else System.nanoTime()
        slot.pendingSeekMs = positionFor(index)
        slot.firstFramePresented = false

        updatePage(index) {
            copy(
                surfaceId = slot.id,
                posterUrl = posterUrlFor(index),
                firstFramePresented = false,
                isPlaying = false,
                isBuffering = true,
                standbyReady = false,
                errorMessage = null,
                startupSource =
                    if (startPaused) DesktopStartupSource.STANDBY else DesktopStartupSource.COLD,
            )
        }

        slot.player.audio().setMute(startPaused || muted)
        if (startPaused) {
            slot.execute(PendingPlayerAction.StartPausedMrl(assignment.sourceUri))
        } else {
            slot.execute(PendingPlayerAction.PlayMrl(assignment.sourceUri))
        }
    }

    private fun prepareAdjacent(
        centerIndex: Int,
        direction: DesktopScrollDirection,
    ) {
        preferredAdjacentIndex(centerIndex, direction, items.lastIndex)?.let(::preloadPage)
    }

    private fun addPlayerListener(slot: DesktopPlayerSlot) {
        val listener =
            object : MediaPlayerEventAdapter() {
                override fun playing(mediaPlayer: MediaPlayer) {
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
                    slot.phase = DesktopSlotPhase.PLAYING
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
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
                    slot.phase = DesktopSlotPhase.READY
                    publish(assignment) {
                        copy(
                            isPlaying = false,
                            standbyReady = slot !== activeSlot,
                            isBuffering = if (slot === activeSlot) isBuffering else false,
                        )
                    }
                }

                override fun stopped(mediaPlayer: MediaPlayer) {
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
                    if (slot.phase != DesktopSlotPhase.RELEASED) slot.phase = DesktopSlotPhase.EMPTY
                    publish(assignment) { copy(isPlaying = false, isBuffering = false) }
                }

                override fun videoOutput(
                    mediaPlayer: MediaPlayer,
                    newCount: Int,
                ) {
                    if (newCount <= 0) return
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
                    if (slot !== activeSlot) slot.phase = DesktopSlotPhase.READY
                    publish(assignment) {
                        copy(standbyReady = slot !== activeSlot, isBuffering = slot === activeSlot)
                    }
                    if (slot === activeSlot && !slot.firstFramePresented) {
                        markFirstFramePresented(slot, assignment)
                    }
                }

                override fun timeChanged(
                    mediaPlayer: MediaPlayer,
                    newTime: Long,
                ) {
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
                    val isCurrentPlayback =
                        slot === activeSlot &&
                            assignment.index == currentIndex
                    if (!isCurrentPlayback) {
                        return
                    }
                    if (newTime > 0L) savedPositionsMs[assignment.videoId] = newTime
                    if (slot.buffering) {
                        slot.buffering = false
                        publish(assignment) {
                            copy(isBuffering = false)
                        }
                    }
                    if (!slot.firstFramePresented) markFirstFramePresented(slot, assignment)
                }

                override fun buffering(
                    mediaPlayer: MediaPlayer,
                    newCache: Float,
                ) {
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
                    val nowBuffering = newCache < BUFFER_COMPLETE_PERCENT
                    val enteredRebuffer =
                        slot === activeSlot && nowBuffering && !slot.buffering && slot.firstFramePresented
                    slot.buffering = nowBuffering
                    publish(assignment) {
                        copy(
                            isBuffering = slot === activeSlot && nowBuffering,
                            rebufferCount = rebufferCount + if (enteredRebuffer) 1 else 0,
                        )
                    }
                }

                override fun finished(mediaPlayer: MediaPlayer) {
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
                    slot.phase = DesktopSlotPhase.ENDED
                    if (slot === activeSlot && assignment.index == currentIndex) {
                        mediaPlayer.submit {
                            mediaPlayer.controls().setTime(0L)
                            mediaPlayer.controls().play()
                        }
                    }
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    val assignment = slot.validAssignment(mediaPlayer) ?: return
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

    private fun markFirstFramePresented(
        slot: DesktopPlayerSlot,
        assignment: DesktopSlotAssignment,
    ) {
        slot.firstFramePresented = true
        slot.buffering = false
        val startupMs =
            slot.startupStartedAtNanos
                .takeIf { it != 0L }
                ?.let { (System.nanoTime() - it) / NANOS_PER_MILLISECOND }
        slot.startupStartedAtNanos = 0L
        publish(assignment) {
            copy(
                firstFramePresented = true,
                isBuffering = false,
                startupTimeMs = startupMs,
            )
        }
    }

    private fun DesktopPlayerSlot.validAssignment(mediaPlayer: MediaPlayer): DesktopSlotAssignment? {
        if (released || player !== mediaPlayer) return null
        val current = assignment
        val currentMrl = runCatching { mediaPlayer.media().info().mrl() }.getOrNull()
        return current?.takeIf { currentMrl == null || currentMrl == it.sourceUri }
    }

    private fun saveActivePosition() {
        val slot = activeSlot ?: return
        val assignment = slot.assignment ?: return
        val position = runCatching { slot.player.status().time() }.getOrDefault(0L)
        if (position > 0L) savedPositionsMs[assignment.videoId] = position
        updatePage(assignment.index) {
            copy(posterUrl = items[assignment.index].thumbnailUrlAt(position))
        }
    }

    private fun positionFor(index: Int): Long = savedPositionsMs[items[index].id] ?: 0L

    private fun posterUrlFor(index: Int): String {
        val positionMs = positionFor(index)
        return if (positionMs > 0L) items[index].thumbnailUrlAt(positionMs) else items[index].images.posterUrl
    }

    private fun slotAssignedTo(index: Int): DesktopPlayerSlot? {
        return slots.firstOrNull { it.assignment?.index == index }
    }

    private fun slotForNewAssignment(): DesktopPlayerSlot {
        return slots.firstOrNull { it !== activeSlot } ?: requireNotNull(activeSlot)
    }

    private fun updatePage(
        index: Int,
        transform: DesktopPagePlaybackState.() -> DesktopPagePlaybackState,
    ) {
        mutableState.update { state ->
            val page = state.pages[index] ?: return@update state
            state.copy(pages = state.pages + (index to page.transform()))
        }
    }

    private fun publish(
        assignment: DesktopSlotAssignment,
        transform: DesktopPagePlaybackState.() -> DesktopPagePlaybackState,
    ) {
        if (released) return
        SwingUtilities.invokeLater {
            if (released || slotAssignedTo(assignment.index)?.assignment != assignment) return@invokeLater
            updatePage(assignment.index, transform)
        }
    }

    internal companion object {
        fun create(items: List<VideoItem>): Result<DesktopPlaybackCoordinator> {
            var factory: MediaPlayerFactory? = null
            return runCatching {
                val createdFactory = MediaPlayerFactory()
                factory = createdFactory
                val slots =
                    List(PLAYER_COUNT) { id ->
                        val component = CallbackMediaPlayerComponent(
                            createdFactory,
                            null,
                            null,
                            true,
                            null
                        ).apply {
                            minimumSize = Dimension(1, 1)
                            preferredSize = Dimension(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT)
                        }
                        val player = component.mediaPlayer()
                        val slot = DesktopPlayerSlot(id = id, component = component, player = player, surface = component)
                        component.addHierarchyListener { _ ->
                            if (component.isDisplayable) {
                                slot.execute(slot.pendingAction)
                            }
                        }
                        slot
                    }
                DesktopPlaybackCoordinator(items = items, factory = createdFactory, slots = slots)
            }.onFailure {
                runCatching { factory?.release() }
            }.recoverCatching { cause ->
                throw IllegalStateException(
                    "LibVLC was not found. Install VLC 3.x with the same CPU architecture as the JDK.",
                    cause,
                )
            }
        }

        private const val PLAYER_COUNT = 2
        private const val NO_ACTIVE_INDEX = -1
        private const val DEFAULT_VIDEO_WIDTH = 540
        private const val DEFAULT_VIDEO_HEIGHT = 960
        private const val BUFFER_COMPLETE_PERCENT = 100f
        private const val NANOS_PER_MILLISECOND = 1_000_000L

        internal val MEDIA_OPTIONS =
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
    val pages: Map<Int, DesktopPagePlaybackState> = emptyMap(),
) {
    val activePage: DesktopPagePlaybackState?
        get() = pages[activeIndex]
}

internal data class DesktopPagePlaybackState(
    val surfaceId: Int? = null,
    val posterUrl: String,
    val firstFramePresented: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val standbyReady: Boolean = false,
    val startupSource: DesktopStartupSource = DesktopStartupSource.COLD,
    val startupTimeMs: Long? = null,
    val rebufferCount: Int = 0,
    val errorCount: Int = 0,
    val errorMessage: String? = null,
)

internal enum class DesktopStartupSource {
    COLD,
    STANDBY,
}

internal enum class DesktopScrollDirection {
    INITIAL,
    FORWARD,
    BACKWARD,
}

internal fun scrollDirection(
    from: Int,
    to: Int,
): DesktopScrollDirection =
    when {
        from < 0 -> DesktopScrollDirection.INITIAL
        to > from -> DesktopScrollDirection.FORWARD
        else -> DesktopScrollDirection.BACKWARD
    }

internal fun preferredAdjacentIndex(
    centerIndex: Int,
    direction: DesktopScrollDirection,
    lastIndex: Int,
): Int? {
    val preferred = if (direction == DesktopScrollDirection.BACKWARD) centerIndex - 1 else centerIndex + 1
    val fallback = if (direction == DesktopScrollDirection.BACKWARD) centerIndex + 1 else centerIndex - 1
    return listOf(preferred, fallback).firstOrNull { it in 0..lastIndex }
}

private data class DesktopSlotAssignment(
    val index: Int,
    val videoId: String,
    val sourceUri: String,
    val generation: Long,
)

private enum class DesktopSlotPhase {
    EMPTY,
    PREPARING,
    READY,
    PLAYING,
    ENDED,
    FAILED,
    RELEASED,
}

internal sealed interface PendingPlayerAction {
    object None : PendingPlayerAction
    data class PlayMrl(val mrl: String) : PendingPlayerAction
    data class StartPausedMrl(val mrl: String) : PendingPlayerAction
    object Resume : PendingPlayerAction
}

private class DesktopPlayerSlot(
    val id: Int,
    val component: CallbackMediaPlayerComponent,
    val player: EmbeddedMediaPlayer,
    val surface: Component,
    var assignment: DesktopSlotAssignment? = null,
    var phase: DesktopSlotPhase = DesktopSlotPhase.EMPTY,
    var pendingSeekMs: Long = 0L,
    var startupStartedAtNanos: Long = 0L,
    var firstFramePresented: Boolean = false,
    var buffering: Boolean = false,
    var listener: MediaPlayerEventAdapter? = null,
) {
    var pendingAction: PendingPlayerAction = PendingPlayerAction.None

    fun execute(action: PendingPlayerAction) {
        if (surface.isDisplayable) {
            pendingAction = PendingPlayerAction.None
            when (action) {
                is PendingPlayerAction.PlayMrl -> {
                    player.media().play(action.mrl, *DesktopPlaybackCoordinator.MEDIA_OPTIONS)
                }
                is PendingPlayerAction.StartPausedMrl -> {
                    player.media().startPaused(action.mrl, *DesktopPlaybackCoordinator.MEDIA_OPTIONS)
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
        if (action is PendingPlayerAction.StartPausedMrl) {
            execute(PendingPlayerAction.PlayMrl(action.mrl))
        } else if (action is PendingPlayerAction.PlayMrl) {
            // Already going to play the media when displayable, nothing to do
        } else {
            execute(PendingPlayerAction.Resume)
        }
    }
}
