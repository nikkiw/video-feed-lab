package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.coordinator

import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics.PlaybackMetricsTracker
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics.ScrollDirection
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.AcquisitionReadiness
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.PlayerPool
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.PlayerPoolEvent
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.policy.PlaybackPolicy
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.preload.FeedPreloader
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.view.PlayerViewBinder
import com.nikkiw.videofeedlab.shared.model.VideoItem
import com.nikkiw.videofeedlab.shared.model.thumbnailUrlAt
import kotlinx.coroutines.flow.StateFlow

internal class AndroidPlaybackCoordinator(
    private val items: List<VideoItem>,
    private val mediaItems: List<MediaItem>,
    private val policy: PlaybackPolicy,
    private val session: PlaybackSessionState,
    dependencies: PlaybackDependencies,
) {
    private val playerPool = dependencies.playerPool
    private val preloader = dependencies.preloader
    private val metrics = dependencies.metrics
    private val viewBinder = dependencies.viewBinder
    private var released = false

    val pages: StateFlow<Map<Int, PagePlaybackState>> = session.pages

    init {
        playerPool.setEventListener(::onPlayerPoolEvent)
    }

    fun play(index: Int) {
        if (released || index !in items.indices) return
        if (index == session.currentIndex && playerPool.activeAssignment?.index == index) {
            playerPool.activePlayer?.playWhenReady = true
            return
        }

        val previousIndex = session.currentIndex
        if (previousIndex in items.indices) {
            playerPool.activePlayer?.let { session.savePosition(items[previousIndex].id, it.currentPosition) }
        }
        val direction = direction(previousIndex, index)
        session.select(index)
        val positionMs = session.positionFor(items[index].id)
        val acquisition = playerPool.acquire(index, mediaItems[index], positionMs)

        session.assign(
            acquisition = acquisition,
            posterUrl = items[index].thumbnailUrlAt(positionMs),
            placeholderUrl = items[index].images.blurredPosterUrl,
        )
        metrics.startupRequested(acquisition.assignment, acquisition.source, direction)
        playerPool.setMuted(session.isMuted)
        acquisition.player.playWhenReady = true
        preloader.updateWindow(index, direction)

        if (acquisition.readiness == AcquisitionReadiness.FIRST_FRAME_AVAILABLE) {
            session.firstFrameRendered(acquisition.assignment)
            metrics.firstFrameRendered(acquisition.assignment, acquisition.player.videoFormat)
        }
        prepareAdjacent(index, direction)
    }

    fun preloadPage(index: Int) {
        if (released || index !in items.indices || index == session.currentIndex) return
        val direction = direction(session.currentIndex, index)
        preloader.updateWindow(session.currentIndex.coerceAtLeast(0), direction)
        val positionMs = session.positionFor(items[index].id)
        playerPool.prepareStandby(index, mediaItems[index], positionMs)?.let { acquisition ->
            session.assign(
                acquisition = acquisition,
                posterUrl = items[index].thumbnailUrlAt(positionMs),
                placeholderUrl = items[index].images.blurredPosterUrl,
            )
        }
    }

    fun bindPlayerView(
        index: Int,
        playerView: PlayerView,
        pageState: PagePlaybackState,
    ) {
        viewBinder.bind(index, playerView, pageState.player)
    }

    fun unbindPlayerView(
        index: Int,
        playerView: PlayerView,
    ) {
        viewBinder.unbind(index, playerView)
    }

    fun onScrollStart() = playerPool.pauseActive()

    fun togglePlayPause() = playerPool.toggleActivePlayback()

    fun setMuted(muted: Boolean) {
        session.setMuted(muted)
        playerPool.setMuted(muted)
    }

    fun release() {
        if (released) return
        released = true
        val index = session.currentIndex
        if (index in items.indices) {
            playerPool.activePlayer?.let { session.savePosition(items[index].id, it.currentPosition) }
        }
        playerPool.setEventListener {}
        viewBinder.release()
        playerPool.release()
        preloader.release()
        metrics.release()
        session.release()
    }

    private fun prepareAdjacent(
        centerIndex: Int,
        direction: ScrollDirection,
    ) {
        if (policy.playerCount < 2) return
        val preferred = if (direction == ScrollDirection.BACKWARD) centerIndex - 1 else centerIndex + 1
        val fallback = if (direction == ScrollDirection.BACKWARD) centerIndex + 1 else centerIndex - 1
        val candidate = listOf(preferred, fallback).firstOrNull { it in items.indices } ?: return
        val positionMs = session.positionFor(items[candidate].id)
        playerPool.prepareStandby(candidate, mediaItems[candidate], positionMs)?.let { acquisition ->
            session.assign(
                acquisition = acquisition,
                posterUrl = items[candidate].thumbnailUrlAt(positionMs),
                placeholderUrl = items[candidate].images.blurredPosterUrl,
            )
        }
    }

    private fun onPlayerPoolEvent(event: PlayerPoolEvent) {
        if (released) return
        when (event) {
            is PlayerPoolEvent.FirstFrame -> {
                session.firstFrameRendered(event.assignment)
                metrics.firstFrameRendered(event.assignment, event.format)
            }
            is PlayerPoolEvent.PlayingChanged ->
                if (event.isForActivePage()) {
                    metrics.playingChanged(event.assignment.mediaId, event.playing)
                }
            is PlayerPoolEvent.Buffering ->
                if (event.isForActivePage()) {
                    metrics.buffering(event.assignment.mediaId)
                }
            is PlayerPoolEvent.Failed ->
                if (event.isForActivePage()) {
                    metrics.error(event.assignment.mediaId, event.error.message ?: "Unknown playback error")
                }
            is PlayerPoolEvent.DroppedFrames -> if (event.isForActivePage()) metrics.droppedFrames(event.count)
        }
    }

    private fun PlayerPoolEvent.isForActivePage(): Boolean =
        assignment == playerPool.activeAssignment && assignment.index == session.currentIndex

    private fun direction(
        from: Int,
        to: Int,
    ): ScrollDirection =
        when {
            from == -1 -> ScrollDirection.INITIAL
            to > from -> ScrollDirection.FORWARD
            else -> ScrollDirection.BACKWARD
        }
}

internal data class PlaybackDependencies(
    val playerPool: PlayerPool,
    val preloader: FeedPreloader,
    val metrics: PlaybackMetricsTracker,
    val viewBinder: PlayerViewBinder,
)
