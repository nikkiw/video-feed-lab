package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.coordinator

import androidx.media3.exoplayer.ExoPlayer
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.PlayerAcquisition
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.SlotAssignment
import com.nikkiw.videofeedlab.shared.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class PagePlaybackState(
    val player: ExoPlayer? = null,
    val posterUrl: String,
    val placeholderUrl: String? = null,
    val assignment: SlotAssignment? = null,
    val firstFrameRendered: Boolean = false,
)

internal class PlaybackSessionState(
    items: List<VideoItem>,
) {
    private val positions = mutableMapOf<String, Long>()
    private val mutablePages =
        MutableStateFlow(
            items.indices.associateWith { index ->
                PagePlaybackState(
                    posterUrl = items[index].images.posterUrl,
                    placeholderUrl = items[index].images.blurredPosterUrl,
                )
            },
        )
    private var released = false

    val pages: StateFlow<Map<Int, PagePlaybackState>> = mutablePages.asStateFlow()
    var currentIndex: Int = -1
        private set
    var isMuted: Boolean = false
        private set

    fun select(index: Int) {
        if (!released) currentIndex = index
    }

    fun setMuted(muted: Boolean) {
        if (!released) isMuted = muted
    }

    fun savePosition(
        videoId: String,
        positionMs: Long,
    ) {
        if (!released) positions[videoId] = positionMs.coerceAtLeast(0L)
    }

    fun positionFor(videoId: String): Long = positions[videoId] ?: 0L

    fun assign(
        acquisition: PlayerAcquisition,
        posterUrl: String,
        placeholderUrl: String?,
    ) {
        if (released) return
        mutablePages.update { pages ->
            pages.mapValues { (index, page) ->
                when {
                    index == acquisition.assignment.index ->
                        page.copy(
                            player = acquisition.player,
                            posterUrl = posterUrl,
                            placeholderUrl = placeholderUrl,
                            assignment = acquisition.assignment,
                            firstFrameRendered =
                                page.assignment == acquisition.assignment && page.firstFrameRendered,
                        )
                    page.player === acquisition.player ->
                        page.copy(player = null, assignment = null, firstFrameRendered = false)
                    else -> page
                }
            }
        }
    }

    fun firstFrameRendered(assignment: SlotAssignment) {
        if (released) return
        mutablePages.update { pages ->
            val page = pages[assignment.index] ?: return@update pages
            if (page.assignment != assignment) {
                pages
            } else {
                pages + (assignment.index to page.copy(firstFrameRendered = true))
            }
        }
    }

    fun release() {
        if (released) return
        released = true
        mutablePages.update { pages ->
            pages.mapValues { (_, page) -> page.copy(player = null, assignment = null, firstFrameRendered = false) }
        }
    }
}
