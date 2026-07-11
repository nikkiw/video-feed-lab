package com.nikkiw.videofeedlab.feature.videofeed.impl.store

import com.arkivanov.mvikotlin.core.store.Store
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.shared.model.VideoItem

interface VideoFeedStore : Store<VideoFeedStore.Intent, VideoFeedStore.State, Nothing> {
    sealed interface Intent {
        data class SelectPage(val index: Int) : Intent

        object ToggleMute : Intent

        object TogglePlay : Intent

        data class UpdateDebugState(val debugState: PlaybackDebugState) : Intent
    }

    data class State(
        val items: List<VideoItem>,
        val activeIndex: Int = 0,
        val isMuted: Boolean = true,
        val isPlaying: Boolean = false,
        val debugState: PlaybackDebugState = PlaybackDebugState(),
    )
}
