package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player

import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics.StartupSource
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.policy.PlaybackPolicy
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.preload.FeedPreloader

internal enum class SlotPhase {
    EMPTY,
    PREPARING,
    READY,
    ENDED,
    FAILED,
    RELEASED,
}

internal data class SlotAssignment(
    val index: Int,
    val mediaId: String,
    val generation: Long,
)

internal data class PlayerSlot(
    val player: ExoPlayer,
    var assignment: SlotAssignment? = null,
    var phase: SlotPhase = SlotPhase.EMPTY,
    var firstFrameGeneration: Long? = null,
)

internal enum class AcquisitionReadiness {
    FIRST_FRAME_AVAILABLE,
    READY,
    PREPARING,
}

internal fun acquisitionReadiness(
    phase: SlotPhase,
    firstFrameGeneration: Long?,
    assignmentGeneration: Long,
): AcquisitionReadiness =
    when {
        firstFrameGeneration == assignmentGeneration -> AcquisitionReadiness.FIRST_FRAME_AVAILABLE
        phase == SlotPhase.READY || phase == SlotPhase.ENDED -> AcquisitionReadiness.READY
        else -> AcquisitionReadiness.PREPARING
    }

internal fun slotPhaseForPlaybackState(playbackState: Int): SlotPhase =
    when (playbackState) {
        Player.STATE_IDLE -> SlotPhase.EMPTY
        Player.STATE_BUFFERING -> SlotPhase.PREPARING
        Player.STATE_READY -> SlotPhase.READY
        Player.STATE_ENDED -> SlotPhase.ENDED
        else -> SlotPhase.EMPTY
    }

internal data class PlayerAcquisition(
    val player: ExoPlayer,
    val assignment: SlotAssignment,
    val source: StartupSource,
    val readiness: AcquisitionReadiness,
)

internal sealed interface PlayerPoolEvent {
    val assignment: SlotAssignment

    data class FirstFrame(
        override val assignment: SlotAssignment,
        val format: Format?,
    ) : PlayerPoolEvent

    data class PlayingChanged(
        override val assignment: SlotAssignment,
        val playing: Boolean,
    ) : PlayerPoolEvent

    data class Buffering(override val assignment: SlotAssignment) : PlayerPoolEvent

    data class Failed(
        override val assignment: SlotAssignment,
        val error: PlaybackException,
    ) : PlayerPoolEvent

    data class DroppedFrames(
        override val assignment: SlotAssignment,
        val count: Int,
    ) : PlayerPoolEvent
}

internal fun interface FeedPlayerFactory {
    fun create(): ExoPlayer
}

internal interface PlayerPool {
    val activePlayer: ExoPlayer?
    val activeAssignment: SlotAssignment?

    fun playerFor(index: Int): ExoPlayer?

    fun acquire(
        index: Int,
        mediaItem: MediaItem,
        positionMs: Long,
    ): PlayerAcquisition

    fun prepareStandby(
        index: Int,
        mediaItem: MediaItem,
        positionMs: Long,
    ): PlayerAcquisition?

    fun pauseActive()

    fun toggleActivePlayback()

    fun setMuted(muted: Boolean)

    fun setEventListener(listener: (PlayerPoolEvent) -> Unit)

    fun release()
}

@OptIn(UnstableApi::class)
internal class Media3FeedPlayerFactory(
    private val builder: DefaultPreloadManager.Builder,
    private val policy: PlaybackPolicy,
) : FeedPlayerFactory {
    override fun create(): ExoPlayer =
        builder.buildExoPlayer().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            trackSelectionParameters =
                trackSelectionParameters
                    .buildUpon()
                    .setViewportSizeToPhysicalDisplaySize(true)
                    .setMaxVideoSize(policy.maxVideoWidth, policy.maxVideoHeight)
                    .build()
        }
}

@OptIn(UnstableApi::class)
internal class Media3PlayerPool(
    playerFactory: FeedPlayerFactory,
    playerCount: Int,
    private val preloader: FeedPreloader,
) : PlayerPool {
    private val slots = List(playerCount) { PlayerSlot(playerFactory.create()) }
    private val indexToSlot = mutableMapOf<Int, PlayerSlot>()
    private var activeSlot: PlayerSlot? = null
    private var generation = 0L
    private var muted = false
    private var released = false
    private var eventListener: (PlayerPoolEvent) -> Unit = {}

    init {
        slots.forEach(::addListeners)
    }

    override val activePlayer: ExoPlayer?
        get() = activeSlot?.player

    override val activeAssignment: SlotAssignment?
        get() = activeSlot?.assignment

    override fun playerFor(index: Int): ExoPlayer? = indexToSlot[index]?.player

    override fun acquire(
        index: Int,
        mediaItem: MediaItem,
        positionMs: Long,
    ): PlayerAcquisition {
        check(!released) { "PlayerPool is released" }
        val reusable = indexToSlot[index]?.takeUnless { it.phase in unusablePhases }
        if (reusable != null) return promote(reusable)

        val target = activeSlot ?: slots.first()
        target.player.pause()
        val source = assign(target, index, mediaItem, positionMs)
        activeSlot = target
        target.player.volume = if (muted) 0f else 1f
        return PlayerAcquisition(
            player = target.player,
            assignment = requireNotNull(target.assignment),
            source = source,
            readiness = AcquisitionReadiness.PREPARING,
        )
    }

    override fun prepareStandby(
        index: Int,
        mediaItem: MediaItem,
        positionMs: Long,
    ): PlayerAcquisition? {
        val standby = slots.firstOrNull { it !== activeSlot }
        return if (released || standby == null) {
            null
        } else {
            val existing = indexToSlot[index]
            if (existing === standby && existing.phase !in unusablePhases) {
                acquisitionFor(existing)
            } else {
                standby.player.volume = 0f
                standby.player.playWhenReady = false
                val source = assign(standby, index, mediaItem, positionMs)
                PlayerAcquisition(
                    player = standby.player,
                    assignment = requireNotNull(standby.assignment),
                    source = source,
                    readiness = AcquisitionReadiness.PREPARING,
                )
            }
        }
    }

    private fun promote(slot: PlayerSlot): PlayerAcquisition {
        if (slot !== activeSlot) {
            activeSlot?.player?.pause()
            activeSlot?.player?.volume = 0f
            activeSlot = slot
        }
        slot.player.volume = if (muted) 0f else 1f
        return acquisitionFor(slot)
    }

    private fun acquisitionFor(slot: PlayerSlot): PlayerAcquisition {
        val assignment = requireNotNull(slot.assignment)
        val readiness = acquisitionReadiness(slot.phase, slot.firstFrameGeneration, assignment.generation)
        val source =
            if (readiness == AcquisitionReadiness.PREPARING) {
                StartupSource.STANDBY_PREPARING
            } else {
                StartupSource.STANDBY_READY
            }
        return PlayerAcquisition(slot.player, assignment, source, readiness)
    }

    private fun assign(
        slot: PlayerSlot,
        index: Int,
        mediaItem: MediaItem,
        positionMs: Long,
    ): StartupSource {
        slot.player.stop()
        slot.assignment?.let { old ->
            if (indexToSlot[old.index] === slot) indexToSlot.remove(old.index)
        }
        val assignment = SlotAssignment(index, mediaItem.mediaId, ++generation)
        slot.assignment = assignment
        slot.phase = SlotPhase.PREPARING
        slot.firstFrameGeneration = null
        indexToSlot[index] = slot

        val preloadedSource = preloader.mediaSourceFor(index)
        if (preloadedSource != null) {
            slot.player.setMediaSource(preloadedSource)
        } else {
            slot.player.setMediaItem(mediaItem)
        }
        slot.player.seekTo(positionMs)
        slot.player.prepare()
        return if (preloadedSource != null) {
            StartupSource.PRELOADED_MEDIA_SOURCE
        } else {
            StartupSource.NETWORK_OR_CACHE
        }
    }

    override fun pauseActive() {
        if (!released) activeSlot?.player?.pause()
    }

    override fun toggleActivePlayback() {
        if (!released) activeSlot?.player?.let { it.playWhenReady = !it.playWhenReady }
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        if (!released) activeSlot?.player?.volume = if (muted) 0f else 1f
    }

    override fun setEventListener(listener: (PlayerPoolEvent) -> Unit) {
        eventListener = if (released) ({}) else listener
    }

    override fun release() {
        if (released) return
        released = true
        eventListener = {}
        indexToSlot.clear()
        slots.forEach { slot ->
            slot.phase = SlotPhase.RELEASED
            slot.assignment = null
            slot.player.release()
        }
        activeSlot = null
    }

    private fun addListeners(slot: PlayerSlot) {
        slot.player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val assignment = slot.assignment ?: return
                    slot.phase = slotPhaseForPlaybackState(playbackState)
                    if (playbackState == Player.STATE_BUFFERING) eventListener(PlayerPoolEvent.Buffering(assignment))
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    validAssignment(slot)?.let { eventListener(PlayerPoolEvent.PlayingChanged(it, isPlaying)) }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val assignment = slot.assignment ?: return
                    slot.phase = SlotPhase.FAILED
                    eventListener(PlayerPoolEvent.Failed(assignment, error))
                }
            },
        )
        slot.player.addAnalyticsListener(
            object : AnalyticsListener {
                override fun onRenderedFirstFrame(
                    eventTime: AnalyticsListener.EventTime,
                    output: Any,
                    renderTimeMs: Long,
                ) {
                    val assignment = assignmentForEvent(slot, eventTime) ?: return
                    slot.firstFrameGeneration = assignment.generation
                    eventListener(PlayerPoolEvent.FirstFrame(assignment, slot.player.videoFormat))
                }

                override fun onDroppedVideoFrames(
                    eventTime: AnalyticsListener.EventTime,
                    droppedFrames: Int,
                    elapsedMs: Long,
                ) {
                    assignmentForEvent(slot, eventTime)?.let {
                        eventListener(PlayerPoolEvent.DroppedFrames(it, droppedFrames))
                    }
                }
            },
        )
    }

    private fun validAssignment(slot: PlayerSlot): SlotAssignment? =
        slot.assignment?.takeIf { assignment ->
            !released && slot.player.currentMediaItem?.mediaId == assignment.mediaId
        }

    private fun assignmentForEvent(
        slot: PlayerSlot,
        eventTime: AnalyticsListener.EventTime,
    ): SlotAssignment? {
        val assignment = slot.assignment ?: return null
        val eventMediaId = eventTime.mediaId()
        return assignment.takeIf { !released && eventMediaId == assignment.mediaId }
    }

    private fun AnalyticsListener.EventTime.mediaId(): String? {
        if (timeline.isEmpty || windowIndex !in 0 until timeline.windowCount) return null
        return timeline.getWindow(windowIndex, Timeline.Window()).mediaItem.mediaId
    }

    private companion object {
        val unusablePhases = setOf(SlotPhase.EMPTY, SlotPhase.FAILED, SlotPhase.RELEASED)
    }
}
