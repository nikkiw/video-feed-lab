package com.nikkiw.videofeedlab.feature.videofeed.impl

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory
import org.koin.dsl.module

val videoFeedModule =
    module {
        single<StoreFactory> { DefaultStoreFactory() }
        single<VideoFeedEntryFactory> { DefaultVideoFeedEntryFactory(get(), get()) }
    }
