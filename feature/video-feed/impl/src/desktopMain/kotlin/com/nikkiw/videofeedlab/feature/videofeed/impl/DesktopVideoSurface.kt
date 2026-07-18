package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseWheelListener
import javax.swing.JPanel

@Composable
internal fun DesktopVideoSurface(
    coordinator: DesktopPlaybackCoordinator,
    onPageDelta: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPageDelta = rememberUpdatedState(onPageDelta)
    val wheelListener =
        remember(coordinator) {
            MouseWheelListener { event ->
                val delta = event.wheelRotation.compareTo(0)
                if (delta != 0) {
                    currentOnPageDelta.value(delta)
                }
            }
        }

    DisposableEffect(coordinator, wheelListener) {
        coordinator.videoComponent.addMouseWheelListener(wheelListener)
        onDispose {
            coordinator.videoComponent.removeMouseWheelListener(wheelListener)
        }
    }

    SwingPanel(
        modifier = modifier,
        background = Color.BLACK,
        factory = {
            JPanel(BorderLayout()).apply {
                background = Color.BLACK
                isOpaque = true
                attach(coordinator)
            }
        },
        update = { host -> host.attach(coordinator) },
    )
}

private fun JPanel.attach(coordinator: DesktopPlaybackCoordinator) {
    val videoComponent = coordinator.videoComponent
    val alreadyAttached = componentCount == 1 && getComponent(0) === videoComponent
    if (alreadyAttached) return

    videoComponent.parent?.remove(videoComponent)
    removeAll()
    add(videoComponent, BorderLayout.CENTER)
    revalidate()
    repaint()
}
