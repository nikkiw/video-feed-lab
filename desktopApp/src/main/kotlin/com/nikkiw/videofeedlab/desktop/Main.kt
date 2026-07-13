package com.nikkiw.videofeedlab.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory
import com.nikkiw.videofeedlab.feature.videofeed.impl.videoFeedModule
import com.nikkiw.videofeedlab.shared.catalog.catalogModule
import org.koin.core.context.startKoin

/**
 * The entry point for the desktop application. Displays the video feed diagnostic screen.
 */
fun main() {
    val koinApplication =
        startKoin {
            modules(catalogModule, videoFeedModule)
        }
    val videoFeedEntryFactory = koinApplication.koin.get<VideoFeedEntryFactory>()
    val lifecycle = LifecycleRegistry()
    val rootContext = DefaultComponentContext(lifecycle = lifecycle)
    val videoFeedEntry = videoFeedEntryFactory.create(rootContext)

    try {
        application {
            Window(onCloseRequest = ::exitApplication, title = "Video Feed Lab") {
                videoFeedEntry.Content()
            }
        }
    } finally {
        koinApplication.close()
    }
}
