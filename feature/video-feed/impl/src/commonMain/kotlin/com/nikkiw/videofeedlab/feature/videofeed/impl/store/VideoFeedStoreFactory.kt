package com.nikkiw.videofeedlab.feature.videofeed.impl.store

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.nikkiw.videofeedlab.feature.videofeed.api.CatalogLoadState
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.shared.catalog.VideoRepository
import com.nikkiw.videofeedlab.shared.model.VideoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VideoFeedStoreFactory(
    private val storeFactory: StoreFactory,
    private val repository: VideoRepository,
    private val initialActiveIndex: Int = 0,
) {
    fun create(): VideoFeedStore =
        object :
            VideoFeedStore,
            Store<VideoFeedStore.Intent, VideoFeedStore.State, Nothing> by storeFactory.create(
                name = "VideoFeedStore",
                initialState =
                    VideoFeedStore.State(
                        activeIndex = initialActiveIndex.coerceAtLeast(0),
                    ),
                bootstrapper = SimpleBootstrapper(Action.LoadCatalog(forceRefresh = false)),
                executorFactory = { ExecutorImpl(repository) },
                reducer = ReducerImpl,
            ) {}

    private sealed interface Action {
        data class LoadCatalog(val forceRefresh: Boolean) : Action
    }

    private sealed interface Msg {
        data object CatalogLoading : Msg

        data class CatalogLoaded(val items: List<VideoItem>) : Msg

        data class CatalogLoadFailed(val message: String) : Msg

        data class PageChanged(val index: Int) : Msg

        object MuteToggled : Msg

        object PlayToggled : Msg

        data class DebugStateUpdated(val debugState: PlaybackDebugState) : Msg
    }

    private class ExecutorImpl(
        private val repository: VideoRepository,
    ) : CoroutineExecutor<VideoFeedStore.Intent, Action, VideoFeedStore.State, Msg, Nothing>() {
        private var catalogJob: Job? = null

        override fun executeAction(action: Action) {
            when (action) {
                is Action.LoadCatalog -> loadCatalog(action.forceRefresh)
            }
        }

        override fun executeIntent(intent: VideoFeedStore.Intent) {
            when (intent) {
                is VideoFeedStore.Intent.SelectPage -> dispatch(Msg.PageChanged(intent.index))
                VideoFeedStore.Intent.ToggleMute -> dispatch(Msg.MuteToggled)
                VideoFeedStore.Intent.TogglePlay -> dispatch(Msg.PlayToggled)
                is VideoFeedStore.Intent.UpdateDebugState -> dispatch(Msg.DebugStateUpdated(intent.debugState))
                VideoFeedStore.Intent.RetryLoad -> loadCatalog(forceRefresh = true)
            }
        }

        private fun loadCatalog(forceRefresh: Boolean) {
            catalogJob?.cancel()
            catalogJob =
                scope.launch {
                    dispatch(Msg.CatalogLoading)
                    try {
                        dispatch(Msg.CatalogLoaded(repository.getVideos(forceRefresh = forceRefresh)))
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        dispatch(Msg.CatalogLoadFailed(CATALOG_LOAD_ERROR_MESSAGE))
                    }
                }
        }
    }

    private object ReducerImpl : Reducer<VideoFeedStore.State, Msg> {
        override fun VideoFeedStore.State.reduce(msg: Msg): VideoFeedStore.State =
            when (msg) {
                Msg.CatalogLoading -> copy(catalogLoadState = CatalogLoadState.Loading)
                is Msg.CatalogLoaded -> {
                    val nextActiveIndex =
                        if (msg.items.isEmpty()) {
                            0
                        } else {
                            activeIndex.coerceIn(msg.items.indices)
                        }
                    copy(
                        items = msg.items,
                        activeIndex = nextActiveIndex,
                        debugState =
                            PlaybackDebugState(
                                videoId =
                                    msg.items.getOrNull(
                                        nextActiveIndex,
                                    )?.id,
                            ),
                        catalogLoadState =
                            if (msg.items.isEmpty()) CatalogLoadState.Empty else CatalogLoadState.Content,
                    )
                }

                is Msg.CatalogLoadFailed ->
                    copy(catalogLoadState = CatalogLoadState.Error(msg.message))

                is Msg.PageChanged ->
                    if (msg.index in items.indices) {
                        copy(
                            activeIndex = msg.index,
                            debugState = PlaybackDebugState(videoId = items[msg.index].id),
                        )
                    } else {
                        this
                    }

                Msg.MuteToggled -> copy(isMuted = !isMuted)
                Msg.PlayToggled -> copy(isPlaying = !isPlaying)
                is Msg.DebugStateUpdated -> copy(debugState = msg.debugState)
            }
    }

    private companion object {
        const val CATALOG_LOAD_ERROR_MESSAGE =
            "Unable to load the video catalog. Check the Media Lab and retry."
    }
}
