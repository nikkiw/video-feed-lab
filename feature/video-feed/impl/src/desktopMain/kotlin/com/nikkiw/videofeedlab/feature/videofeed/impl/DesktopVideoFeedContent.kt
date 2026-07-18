package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent
import com.nikkiw.videofeedlab.shared.model.VideoItem
import kotlinx.coroutines.launch

@Composable
internal fun DesktopVideoFeedContent(
    component: VideoFeedComponent,
    model: VideoFeedComponent.Model,
) {
    val coordinatorResult = remember(model.items) { DesktopPlaybackCoordinator.create(model.items) }
    val coordinator = coordinatorResult.getOrNull()
    if (coordinator == null) {
        DesktopPlaybackUnavailable(
            message = coordinatorResult.exceptionOrNull()?.message ?: "Desktop playback is unavailable",
        )
        return
    }

    val playback by coordinator.state.collectAsState()
    val pagerState =
        rememberPagerState(
            initialPage = model.activeIndex.coerceIn(0, model.items.lastIndex),
            pageCount = { model.items.size },
        )
    val scope = rememberCoroutineScope()

    fun moveBy(delta: Int) {
        val target = (pagerState.settledPage + delta).coerceIn(model.items.indices)
        if (target != pagerState.settledPage) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    DisposableEffect(coordinator) {
        coordinator.setMuted(model.isMuted)
        onDispose(coordinator::release)
    }

    LaunchedEffect(model.isMuted, coordinator) {
        coordinator.setMuted(model.isMuted)
    }

    LaunchedEffect(pagerState.settledPage, coordinator) {
        component.onPageSelected(pagerState.settledPage)
        coordinator.play(pagerState.settledPage)
    }

    LaunchedEffect(pagerState.isScrollInProgress, coordinator) {
        if (pagerState.isScrollInProgress) {
            coordinator.onScrollStart()
        } else {
            coordinator.play(pagerState.settledPage)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        DesktopToolbar(
            activeIndex = pagerState.settledPage,
            itemCount = model.items.size,
            isMuted = model.isMuted,
            playback = playback,
            onPrevious = { moveBy(-1) },
            onNext = { moveBy(1) },
            onTogglePlay = {
                coordinator.togglePlayPause()
                component.onTogglePlay()
            },
            onToggleMuted = component::onToggleMute,
        )

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            DesktopFeedPage(
                item = model.items[page],
                isActive = page == pagerState.settledPage,
                coordinator = coordinator,
                playback = playback,
                onPageDelta = ::moveBy,
                onTogglePlay = {
                    coordinator.togglePlayPause()
                    component.onTogglePlay()
                },
            )
        }
    }
}

@Composable
private fun DesktopToolbar(
    activeIndex: Int,
    itemCount: Int,
    isMuted: Boolean,
    playback: DesktopPlaybackState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleMuted: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF101014))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Video Feed Lab", color = Color.White)
        Button(onClick = onPrevious, enabled = activeIndex > 0) { Text("Previous") }
        Button(onClick = onTogglePlay) { Text(if (playback.isPlaying) "Pause" else "Play") }
        Button(onClick = onNext, enabled = activeIndex < itemCount - 1) { Text("Next") }
        Button(onClick = onToggleMuted) { Text(if (isMuted) "Unmute" else "Mute") }
        Spacer(modifier = Modifier.weight(1f))

        val status =
            when {
                playback.errorMessage != null -> "error"
                playback.isBuffering -> "buffering"
                playback.isPlaying -> "playing"
                else -> "paused"
            }
        Text(
            text =
                "${activeIndex + 1}/$itemCount · $status" +
                    (playback.startupTimeMs?.let { " · start ${it}ms" } ?: ""),
            color = Color.LightGray,
        )
    }
}

@Composable
private fun DesktopFeedPage(
    item: VideoItem,
    isActive: Boolean,
    coordinator: DesktopPlaybackCoordinator,
    playback: DesktopPlaybackState,
    onPageDelta: (Int) -> Unit,
    onTogglePlay: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (isActive) {
                DesktopVideoSurface(
                    coordinator = coordinator,
                    onPageDelta = onPageDelta,
                    modifier = Modifier.fillMaxSize(),
                )
                if (playback.isBuffering && !playback.firstFrameRendered) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161A))
                    .clickable(onClick = onTogglePlay)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = item.title, color = Color.White)
            Text(text = item.subtitle, color = Color.LightGray)
            Text(
                text =
                    item.source.streamType.name +
                        if (isActive && playback.rebufferCount > 0) {
                            " · rebuffers ${playback.rebufferCount}"
                        } else {
                            ""
                        },
                color = Color.Gray,
            )
            playback.errorMessage?.takeIf { isActive }?.let { message ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message, color = Color(0xFFFF8A80))
            }
        }
    }
}

@Composable
private fun DesktopPlaybackUnavailable(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, color = Color.White)
    }
}
