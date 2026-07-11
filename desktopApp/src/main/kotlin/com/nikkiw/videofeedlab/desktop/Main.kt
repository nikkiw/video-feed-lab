package com.nikkiw.videofeedlab.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.nikkiw.videofeedlab.feature.videofeed.impl.createVideoFeedEntry

/**
 * The entry point for the desktop application. Displays the video feed diagnostic screen.
 */
fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Video Feed Lab") {
            val videoFeedEntry = createVideoFeedEntry()
            videoFeedEntry.content()
        }
    }
