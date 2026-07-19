package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedLaunchParams
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent
import com.nikkiw.videofeedlab.feature.videofeed.impl.store.VideoFeedStore
import com.nikkiw.videofeedlab.feature.videofeed.impl.store.VideoFeedStoreFactory
import com.nikkiw.videofeedlab.shared.catalog.VideoRepository
import com.nikkiw.videofeedlab.shared.mvikotlin.asValue

class DefaultVideoFeedComponent(
    componentContext: ComponentContext,
    storeFactory: StoreFactory,
    repository: VideoRepository,
    launchParams: FeedLaunchParams,
) : VideoFeedComponent, ComponentContext by componentContext {
    private val store =
        instanceKeeper.getStore {
            VideoFeedStoreFactory(
                storeFactory = storeFactory,
                repository = repository,
                initialActiveIndex = launchParams.startIndex,
            ).create()
        }

    override val models: Value<VideoFeedComponent.Model> =
        store
            .asValue()
            .map { value ->
                VideoFeedComponent.Model(
                    items = value.items,
                    activeIndex = value.activeIndex,
                    isMuted = value.isMuted,
                    isPlaying = value.isPlaying,
                    debugState = value.debugState,
                    catalogLoadState = value.catalogLoadState,
                    presentationId = launchParams.presentationId,
                )
            }

    fun updateDebugInfo(debugState: PlaybackDebugState) {
        store.accept(VideoFeedStore.Intent.UpdateDebugState(debugState))
    }

    override fun onPageSelected(index: Int) {
        store.accept(VideoFeedStore.Intent.SelectPage(index))
    }

    override fun onToggleMute() {
        store.accept(VideoFeedStore.Intent.ToggleMute)
    }

    override fun onTogglePlay() {
        store.accept(VideoFeedStore.Intent.TogglePlay)
    }

    override fun onRetryLoad() {
        store.accept(VideoFeedStore.Intent.RetryLoad)
    }
}
