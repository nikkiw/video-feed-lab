package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.nikkiw.videofeedlab.shared.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import java.awt.Component
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean

/** Screen-scoped two-slot LibVLC pool backed by one native LibVLC instance. */
internal class DesktopPlaybackCoordinator private constructor(
    internal val items: List<VideoItem>,
    private val factory: MediaPlayerFactory,
    internal val slots: List<DesktopPlayerSlot>,
    internal val config: DesktopPlaybackConfig,
    private val eventLoop: DesktopPlaybackEventLoop,
) {
    internal val savedPositionsMs = mutableMapOf<String, Long>()
    internal val mutableState =
        MutableStateFlow(
            DesktopPlaybackState(
                pages =
                    items.indices.associateWith { index ->
                        DesktopPagePlaybackState(posterUrl = items[index].images.posterUrl)
                    },
            ),
        )

    val state: StateFlow<DesktopPlaybackState> = mutableState.asStateFlow()

    internal var activeSlot: DesktopPlayerSlot? = null
    internal var currentIndex = NO_ACTIVE_INDEX
    internal var generation = 0L
    internal var muted = false
    internal var scrolling = false
    internal var released = false

    @Volatile
    internal var activeSlotId: Int? = null

    private val releaseRequested = AtomicBoolean(false)

    fun videoComponent(surfaceId: Int): Component? = slots.getOrNull(surfaceId)?.surface

    private fun onSurfaceDisplayable(slot: DesktopPlayerSlot) {
        if (releaseRequested.get()) return
        eventLoop.dispatch {
            if (!released && !releaseRequested.get()) {
                slot.execute(slot.pendingAction)
            }
        }
    }

    fun play(index: Int) {
        if (index !in items.indices || releaseRequested.get()) return
        eventLoop.dispatch { playOnEventLoop(index) }
    }

    fun preloadPage(index: Int) {
        if (index !in items.indices || releaseRequested.get()) return
        eventLoop.dispatch { preloadPageOnEventLoop(index) }
    }

    fun onScrollStart() {
        if (releaseRequested.get()) return
        eventLoop.dispatch(::onScrollStartOnEventLoop)
    }

    fun togglePlayPause() {
        if (releaseRequested.get()) return
        eventLoop.dispatch(::togglePlayPauseOnEventLoop)
    }

    fun setMuted(value: Boolean) {
        if (releaseRequested.get()) return
        eventLoop.dispatch { setMutedOnEventLoop(value) }
    }

    fun release() {
        if (!releaseRequested.compareAndSet(false, true)) return
        eventLoop.shutdown(::releaseOnEventLoop)
    }

    internal fun dispatchCallback(task: () -> Unit): Boolean {
        if (releaseRequested.get()) return false
        return eventLoop.dispatch(task)
    }

    internal fun assertEventLoopThread() {
        eventLoop.assertOwnerThread()
    }

    internal fun isCurrentAssignment(
        slot: DesktopPlayerSlot,
        assignment: DesktopSlotAssignment,
    ): Boolean =
        !releaseRequested.get() &&
            slot.assignmentGeneration() == assignment.generation

    internal fun isCurrentActiveAssignment(
        slot: DesktopPlayerSlot,
        assignment: DesktopSlotAssignment,
    ): Boolean =
        isCurrentAssignment(slot, assignment) &&
            activeSlotId == slot.id

    private fun playOnEventLoop(index: Int) {
        assertEventLoopThread()
        if (released || releaseRequested.get() || index !in items.indices) return
        scrolling = false
        mutableState.update(DesktopPlaybackState::withoutScrollFrames)

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
            trace(old, "pause-command", "reason=switch-active")
            old.player.controls().pause()
            old.player.audio().setMute(true)
        }
        currentIndex = index
        activeSlot = target
        activeSlotId = target.id
        mutableState.update { it.copy(activeIndex = index) }

        val assignedToTarget =
            target.assignment?.index == index &&
                target.phase != DesktopSlotPhase.FAILED

        val readyForInstantPromotion =
            canPromoteWithoutBuffering(
                assignedIndex = target.assignment?.index,
                requestedIndex = index,
                phase = target.phase,
                frameReady = target.frameReady,
            )

        when {
            !assignedToTarget -> {
                trace(target, "promote-cold", "requested=$index")
                assign(
                    slot = target,
                    index = index,
                    startPaused = false,
                )
            }

            readyForInstantPromotion -> {
                trace(target, "promote-ready", "requested=$index")
                promoteReadyStandby(
                    slot = target,
                    index = index,
                )
            }

            else -> {
                trace(target, "promote-warming", "requested=$index")
                promoteWarmingStandby(
                    slot = target,
                    index = index,
                )
            }
        }

        prepareAdjacent(index, direction)
    }

    internal fun preloadPageOnEventLoop(index: Int) {
        assertEventLoopThread()
        if (released || releaseRequested.get()) return
        if (index !in items.indices || index == currentIndex) return
        val existing = slotAssignedTo(index)
        val alreadyPrepared =
            existing != null && existing !== activeSlot && existing.phase != DesktopSlotPhase.FAILED
        if (!alreadyPrepared) {
            slots.firstOrNull { it !== activeSlot }?.let { standby ->
                assign(standby, index, startPaused = true)
            }
        }
    }

    private fun onScrollStartOnEventLoop() {
        assertEventLoopThread()
        if (released || releaseRequested.get() || scrolling) return
        scrolling = true
        activeSlot?.let { trace(it, "pause-command", "reason=scroll-start") }
        activeSlot?.capturePresentedFrame()?.let { captured ->
            updatePage(captured.index) { copy(scrollFrame = captured.frame) }
        }
        saveActivePosition()
        activeSlot?.player?.controls()?.pause()
    }

    private fun togglePlayPauseOnEventLoop() {
        assertEventLoopThread()
        if (released || releaseRequested.get() || currentIndex !in items.indices) return
        activeSlot?.player?.let { player ->
            if (player.status().isPlaying) player.controls().pause() else player.controls().play()
        }
    }

    private fun setMutedOnEventLoop(value: Boolean) {
        assertEventLoopThread()
        if (released || releaseRequested.get()) return
        muted = value
        slots.forEach { slot -> slot.player.audio().setMute(slot !== activeSlot || value) }
    }

    private fun releaseOnEventLoop() {
        assertEventLoopThread()
        if (released) return
        released = true
        activeSlotId = null
        saveActivePosition()
        slots.forEach { slot ->
            slot.assignment = null
            slot.pendingAction = PendingPlayerAction.None
            slot.listener?.let { listener ->
                runCatching { slot.player.events().removeMediaPlayerEventListener(listener) }
            }
            runCatching { slot.player.controls().stop() }
            runCatching { slot.component.release() }
            slot.phase = DesktopSlotPhase.RELEASED
        }
        runCatching { factory.release() }
    }

    internal companion object {
        fun create(
            items: List<VideoItem>,
            config: DesktopPlaybackConfig = DesktopPlaybackConfig(),
        ): Result<DesktopPlaybackCoordinator> {
            var factory: MediaPlayerFactory? = null
            var eventLoop: DesktopPlaybackEventLoop? = null
            return runCatching {
                val createdFactory = MediaPlayerFactory()
                factory = createdFactory
                val createdEventLoop = SingleThreadDesktopPlaybackEventLoop()
                eventLoop = createdEventLoop
                val slots =
                    List(PLAYER_COUNT) { id ->
                        val component =
                            CallbackMediaPlayerComponent(
                                createdFactory,
                                null,
                                null,
                                true,
                                null,
                            ).apply {
                                minimumSize = Dimension(1, 1)
                                preferredSize = Dimension(DEFAULT_VIDEO_WIDTH, DEFAULT_VIDEO_HEIGHT)
                            }
                        val player = component.mediaPlayer()
                        DesktopPlayerSlot(
                            id = id,
                            component = component,
                            player = player,
                            surface = component,
                        )
                    }
                DesktopPlaybackCoordinator(
                    items = items,
                    factory = createdFactory,
                    slots = slots,
                    config = config,
                    eventLoop = createdEventLoop,
                ).also { coordinator ->
                    slots.forEach { slot ->
                        slot.component.addHierarchyListener { _ ->
                            if (slot.component.isDisplayable) {
                                coordinator.onSurfaceDisplayable(slot)
                            }
                        }
                    }
                }
            }.onFailure {
                eventLoop?.shutdown {}
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
    }
}

internal const val BUFFER_COMPLETE_PERCENT = 100f
internal const val NANOS_PER_MILLISECOND = 1_000_000L
