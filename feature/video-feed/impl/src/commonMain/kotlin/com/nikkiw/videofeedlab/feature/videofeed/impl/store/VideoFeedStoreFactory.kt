package com.nikkiw.videofeedlab.feature.videofeed.impl.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.shared.model.VideoItem

class VideoFeedStoreFactory(
    private val storeFactory: StoreFactory,
    private val items: List<VideoItem>,
) {
    fun create(): VideoFeedStore =
        object :
            VideoFeedStore,
            Store<VideoFeedStore.Intent, VideoFeedStore.State, Nothing> by storeFactory.create(
                name = "VideoFeedStore",
                initialState = VideoFeedStore.State(items = items),
                bootstrapper = SimpleBootstrapper(),
                executorFactory = ::ExecutorImpl,
                reducer = ReducerImpl,
            ) {}

    private sealed interface Msg {
        data class PageChanged(val index: Int) : Msg

        object MuteToggled : Msg

        object PlayToggled : Msg

        data class DebugStateUpdated(val debugState: PlaybackDebugState) : Msg
    }

    private class ExecutorImpl : CoroutineExecutor<VideoFeedStore.Intent, Unit, VideoFeedStore.State, Msg, Nothing>() {
        override fun executeIntent(intent: VideoFeedStore.Intent) {
            when (intent) {
                is VideoFeedStore.Intent.SelectPage -> dispatch(Msg.PageChanged(intent.index))
                VideoFeedStore.Intent.ToggleMute -> dispatch(Msg.MuteToggled)
                VideoFeedStore.Intent.TogglePlay -> dispatch(Msg.PlayToggled)
                is VideoFeedStore.Intent.UpdateDebugState -> dispatch(Msg.DebugStateUpdated(intent.debugState))
            }
        }
    }

    private object ReducerImpl : Reducer<VideoFeedStore.State, Msg> {
        override fun VideoFeedStore.State.reduce(msg: Msg): VideoFeedStore.State =
            when (msg) {
                is Msg.PageChanged ->
                    copy(
                        activeIndex = msg.index,
                        debugState = PlaybackDebugState(videoId = items.getOrNull(msg.index)?.id),
                    )
                Msg.MuteToggled -> copy(isMuted = !isMuted)
                Msg.PlayToggled -> copy(isPlaying = !isPlaying)
                is Msg.DebugStateUpdated -> copy(debugState = msg.debugState)
            }
    }
}
