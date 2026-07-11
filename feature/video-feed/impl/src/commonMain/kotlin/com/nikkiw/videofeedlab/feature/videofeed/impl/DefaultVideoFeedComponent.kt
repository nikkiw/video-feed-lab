package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent
import com.nikkiw.videofeedlab.feature.videofeed.impl.store.VideoFeedStore
import com.nikkiw.videofeedlab.feature.videofeed.impl.store.VideoFeedStoreFactory
import com.nikkiw.videofeedlab.shared.model.VideoItem

class DefaultVideoFeedComponent(
    componentContext: ComponentContext,
    private val items: List<VideoItem>,
) : VideoFeedComponent, ComponentContext by componentContext {
    private val store =
        instanceKeeper.getStore {
            VideoFeedStoreFactory(
                storeFactory = DefaultStoreFactory(),
                items = items,
            ).create()
        }

    private val _models =
        MutableValue(
            VideoFeedComponent.Model(
                items = store.state.items,
                activeIndex = store.state.activeIndex,
                isMuted = store.state.isMuted,
                debugState = store.state.debugState,
            ),
        )
    override val models: Value<VideoFeedComponent.Model> = _models

    init {
        store.states(
            object : com.arkivanov.mvikotlin.core.rx.Observer<VideoFeedStore.State> {
                override fun onComplete() = Unit

                override fun onNext(value: VideoFeedStore.State) {
                    _models.value =
                        VideoFeedComponent.Model(
                            items = value.items,
                            activeIndex = value.activeIndex,
                            isMuted = value.isMuted,
                            debugState = value.debugState,
                        )
                }
            },
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
}
