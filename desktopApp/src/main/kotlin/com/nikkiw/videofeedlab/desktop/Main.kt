package com.nikkiw.videofeedlab.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.nikkiw.videofeedlab.feature.videofeed.impl.createVideoFeedEntry

/**
 * The entry point for the desktop application. Displays the video feed diagnostic screen.
 */
fun main() {
    val lifecycle = LifecycleRegistry()
    val rootContext = DefaultComponentContext(lifecycle = lifecycle)

    application {
        Window(onCloseRequest = ::exitApplication, title = "Video Feed Lab") {
            val videoFeedEntry = remember { createVideoFeedEntry(rootContext) }
            videoFeedEntry.content()
        }
    }
}
