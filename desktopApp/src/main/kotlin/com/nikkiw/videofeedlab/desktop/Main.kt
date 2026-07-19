package com.nikkiw.videofeedlab.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory
import com.nikkiw.videofeedlab.feature.videofeed.impl.videoFeedModule
import com.nikkiw.videofeedlab.shared.catalog.catalogModule
import org.koin.core.context.startKoin
import java.util.concurrent.FutureTask
import javax.swing.SwingUtilities

/**
* The entry point for the desktop application.
*/
fun main() {
    // Allow Compose posters and loading indicators to render above LibVLC Swing surfaces.
    System.setProperty("compose.interop.blending", "true")

    val koinApplication =
        startKoin {
            modules(catalogModule, videoFeedModule)
        }

    val lifecycleRegistry = LifecycleRegistry()
    val videoFeedEntryFactory =
        koinApplication.koin.get<VideoFeedEntryFactory>()

    val appRoot =
        runOnUiThread {
            DesktopAppRoot(
                componentContext =
                    DefaultComponentContext(
                        lifecycle = lifecycleRegistry,
                    ),
                videoFeedEntryFactory = videoFeedEntryFactory,
            )
        }

    try {
        application {
            val windowState = rememberWindowState()

            LifecycleController(
                lifecycleRegistry = lifecycleRegistry,
                windowState = windowState,
            )

            Window(
                state = windowState,
                onCloseRequest = ::exitApplication,
                title = "Video Feed Lab",
            ) {
                MaterialTheme {
                    appRoot.Content()
                }
            }
        }
    } finally {
        koinApplication.close()
    }
}

private fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    val task = FutureTask(block)
    SwingUtilities.invokeAndWait(task)
    return task.get()
}
