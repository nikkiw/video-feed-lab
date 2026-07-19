package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent
import com.nikkiw.videofeedlab.shared.model.VideoItem

@Composable
internal fun DesktopVideoFeedContent(
    component: VideoFeedComponent,
    model: VideoFeedComponent.Model,
    onBack: (() -> Unit)?,
) {
    val coordinatorResult = remember(model.items) { DesktopPlaybackCoordinator.create(model.items) }
    val coordinator = coordinatorResult.getOrNull()
    if (coordinator == null) {
        DesktopPlaybackUnavailable(
            message =
                coordinatorResult.exceptionOrNull()?.message
                    ?: "Desktop playback is unavailable",
        )
        return
    }
    val posterLoader = remember(coordinator) { DesktopPosterLoader() }
    val playback by coordinator.state.collectAsState()
    LaunchedEffect(playback, model.items, component) {
        component.onPlaybackDebugStateChanged(
            playback.toPlaybackDebugState(
                videoIds = model.items.map(VideoItem::id),
            ),
        )
    }
    val pagerState =
        rememberPagerState(
            initialPage = model.activeIndex.coerceIn(0, model.items.lastIndex),
            pageCount = { model.items.size },
        )
    val navigation =
        rememberDesktopPagerNavigation(
            pagerState = pagerState,
            itemIndices = model.items.indices,
            onScrollStart = coordinator::onScrollStart,
            onTargetPage = coordinator::preloadPage,
        )

    DesktopPlaybackEffects(
        component = component,
        isMuted = model.isMuted,
        itemIndices = model.items.indices,
        pagerState = pagerState,
        coordinator = coordinator,
        posterLoader = posterLoader,
    )

    val actions =
        remember(navigation, coordinator, component, onBack) {
            DesktopToolbarActions(
                onPrevious = { navigation.moveBy(-1) },
                onNext = { navigation.moveBy(1) },
                onTogglePlay = coordinator::togglePlayPause,
                onToggleMuted = component::onToggleMute,
                onBack = onBack,
            )
        }

    DesktopVideoFeedLayout(
        model = model,
        pagerState = pagerState,
        coordinator = coordinator,
        playback = playback,
        posterLoader = posterLoader,
        actions = actions,
        onWheel = navigation::onWheel,
    )
}

@Composable
internal fun DesktopVideoFeedLayout(
    model: VideoFeedComponent.Model,
    pagerState: PagerState,
    coordinator: DesktopPlaybackCoordinator,
    playback: DesktopPlaybackState,
    posterLoader: DesktopPosterLoader,
    actions: DesktopToolbarActions,
    onWheel: (Double) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DesktopToolbar(
            activeIndex = pagerState.settledPage,
            itemCount = model.items.size,
            isMuted = model.isMuted,
            playback = playback,
            actions = actions,
        )

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            DesktopFixedVideoSurfaces(
                playback = playback,
                coordinator = coordinator,
                onWheel = onWheel,
            )
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                DesktopFeedPage(
                    ui =
                        DesktopFeedPageUi(
                            item = model.items[page],
                            isActive = page == pagerState.settledPage,
                            isScrolling = pagerState.isScrollInProgress,
                            playback = playback.pages.getValue(page),
                        ),
                    posterLoader = posterLoader,
                    onTogglePlay = actions.onTogglePlay,
                )
            }
        }
    }
}

@Composable
internal fun BoxScope.DesktopFixedVideoSurfaces(
    playback: DesktopPlaybackState,
    coordinator: DesktopPlaybackCoordinator,
    onWheel: (Double) -> Unit,
) {
    /* SwingPanel is rendered above Compose content on desktop. Keep the native
     * surface offscreen until the target has a real frame, otherwise it hides the
     * poster/loader and can expose the slot's previous video's last frame. */
    val activeSurfaceId = playback.activePage?.takeIf { it.frameReady }?.surfaceId
    playback.pages.values.mapNotNull(DesktopPagePlaybackState::surfaceId).distinct()
        .forEach { surfaceId ->
            key(surfaceId) {
                val isActive = surfaceId == activeSurfaceId
                DesktopVideoSurface(
                    coordinator = coordinator,
                    surfaceId = surfaceId,
                    onWheel = if (isActive) onWheel else IGNORE_WHEEL,
                    modifier =
                        if (isActive) {
                            Modifier.fillMaxSize().padding(bottom = DESKTOP_METADATA_HEIGHT)
                        } else {
                            Modifier.size(1.dp).align(Alignment.TopStart)
                        },
                )
            }
        }
}

@Composable
internal fun DesktopPlaybackEffects(
    component: VideoFeedComponent,
    isMuted: Boolean,
    itemIndices: IntRange,
    pagerState: PagerState,
    coordinator: DesktopPlaybackCoordinator,
    posterLoader: DesktopPosterLoader,
) {
    DisposableEffect(coordinator, posterLoader) {
        coordinator.setMuted(isMuted)
        onDispose {
            posterLoader.close()
            coordinator.release()
        }
    }
    LaunchedEffect(isMuted, coordinator) {
        coordinator.setMuted(isMuted)
    }
    LaunchedEffect(pagerState.settledPage, coordinator) {
        component.onPageSelected(pagerState.settledPage)
        coordinator.play(pagerState.settledPage)
    }
    var wasScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.isScrollInProgress, coordinator) {
        if (pagerState.isScrollInProgress) {
            wasScrolling = true
            coordinator.onScrollStart()
        } else if (wasScrolling) {
            coordinator.play(pagerState.settledPage)
        }
    }
    LaunchedEffect(pagerState.targetPage, coordinator) {
        val targetPage = pagerState.targetPage
        if (targetPage != pagerState.settledPage && targetPage in itemIndices) {
            coordinator.preloadPage(targetPage)
        }
    }
}

@Composable
internal fun DesktopToolbar(
    activeIndex: Int,
    itemCount: Int,
    isMuted: Boolean,
    playback: DesktopPlaybackState,
    actions: DesktopToolbarActions,
) {
    val activePlayback = playback.activePage
    Row(
        modifier =
            Modifier.fillMaxWidth().background(Color(0xFF101014))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (actions.onBack != null) {
            Button(onClick = actions.onBack) {
                Text("Back")
            }
        }
        Text("Video Feed Lab", color = Color.White)
        Button(onClick = actions.onPrevious, enabled = activeIndex > 0) { Text("Previous") }
        Button(onClick = actions.onTogglePlay) {
            Text(if (activePlayback?.isPlaying == true) "Pause" else "Play")
        }
        Button(onClick = actions.onNext, enabled = activeIndex < itemCount - 1) { Text("Next") }
        Button(onClick = actions.onToggleMuted) { Text(if (isMuted) "Unmute" else "Mute") }
        Spacer(modifier = Modifier.weight(1f))

        val status = activePlayback.statusLabel()
        val source =
            when (activePlayback?.startupSource) {
                DesktopStartupSource.STANDBY ->
                    if (activePlayback.frameReady) {
                        "standby-ready"
                    } else {
                        "standby-warming"
                    }

                DesktopStartupSource.COLD -> "cold-start"
                null -> null
            }
        Text(
            text =
                "${activeIndex + 1}/$itemCount · $status" +
                    (source?.let { " · $it" } ?: "") +
                    (activePlayback?.startupTimeMs?.let { " · start ${it}ms" } ?: "") +
                    (activePlayback?.preloadTimeMs?.let { " · preload ${it}ms" } ?: ""),
            color = Color.LightGray,
        )
    }
}

internal fun DesktopPagePlaybackState?.statusLabel(): String =
    when {
        this?.errorMessage != null -> "error"
        this?.isBuffering == true -> "buffering"
        this?.isPlaying == true -> "playing"
        else -> "paused"
    }

internal data class DesktopToolbarActions(
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onToggleMuted: () -> Unit,
    val onBack: (() -> Unit)?,
)

internal data class DesktopFeedPageUi(
    val item: VideoItem,
    val isActive: Boolean,
    val isScrolling: Boolean,
    val playback: DesktopPagePlaybackState,
)

@Composable
internal fun DesktopFeedPage(
    ui: DesktopFeedPageUi,
    posterLoader: DesktopPosterLoader,
    onTogglePlay: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            ui.playback.scrollFrame?.takeIf { ui.isScrolling }?.let { frame ->
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            VideoLoadingOverlay(
                firstFramePresented = ui.playback.frameReady,
                active = ui.isActive,
                forcePosterVisible = ui.isScrolling && ui.playback.scrollFrame == null,
            ) {
                DesktopPosterImage(
                    loader = posterLoader,
                    posterUrl = ui.playback.posterUrl,
                    fallbackUrl = ui.item.images.posterUrl,
                )
            }
        }

        Column(
            modifier =
                Modifier.fillMaxWidth().height(DESKTOP_METADATA_HEIGHT)
                    .background(Color(0xFF16161A)).clickable(onClick = onTogglePlay)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = ui.item.title, color = Color.White)
            Text(text = ui.item.subtitle, color = Color.LightGray)
            Text(
                text =
                    ui.item.source.streamType.name +
                        if (ui.isActive && ui.playback.rebufferCount > 0) {
                            " · rebuffers ${ui.playback.rebufferCount}"
                        } else {
                            ""
                        },
                color = Color.Gray,
            )
            ui.playback.errorMessage?.takeIf { ui.isActive }?.let { message ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message, color = Color(0xFFFF8A80))
            }
        }
    }
}

internal val IGNORE_WHEEL: (Double) -> Unit = {}
internal val DESKTOP_METADATA_HEIGHT = 104.dp

@Composable
internal fun DesktopPlaybackUnavailable(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, color = Color.White)
    }
}
