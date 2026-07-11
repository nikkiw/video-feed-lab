package com.nikkiw.videofeedlab.feature.videofeed.impl

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent
import com.nikkiw.videofeedlab.shared.model.VideoItem

@Composable
internal actual fun PlatformVideoFeedScreen(component: VideoFeedComponent) {
    val model by component.models.subscribeAsState()
    if (model.items.isEmpty()) return

    val context = LocalContext.current
    val coordinator =
        remember(model.items) {
            AndroidPlaybackCoordinator(
                context = context,
                items = model.items,
                onDebugState = { debug ->
                    (component as? DefaultVideoFeedComponent)?.updateDebugInfo(debug)
                },
            )
        }

    val pagerState = rememberPagerState(pageCount = { model.items.size })

    DisposableEffect(coordinator) {
        coordinator.setMuted(model.isMuted)
        onDispose(coordinator::release)
    }

    LaunchedEffect(model.isMuted) {
        coordinator.setMuted(model.isMuted)
    }

    LaunchedEffect(pagerState.settledPage, coordinator) {
        component.onPageSelected(pagerState.settledPage)
        coordinator.play(pagerState.settledPage)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            VideoFeedItemView(
                item = model.items[page],
                active = page == pagerState.settledPage,
                isFirstFrameRendered = coordinator.isFirstFrameRendered,
                coordinator = coordinator,
                onTogglePlay = { component.onTogglePlay() },
            )
        }

        DebugOverlay(
            state = model.debugState,
            muted = model.isMuted,
            onToggleMuted = { component.onToggleMute() },
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
        )
    }
}

@Composable
private fun VideoFeedItemView(
    item: VideoItem,
    active: Boolean,
    isFirstFrameRendered: Boolean,
    coordinator: AndroidPlaybackCoordinator,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(active) {
                    detectTapGestures(onTap = { onTogglePlay() })
                },
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams =
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                }
            },
            update = { playerView ->
                val targetPlayer = if (active) coordinator.player else null
                playerView.player = targetPlayer
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (active && !isFirstFrameRendered) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
            )
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = item.title, color = Color.White)
            Text(text = item.subtitle, color = Color.LightGray)
            Text(text = item.source.streamType.name, color = Color.LightGray)
        }
    }
}

@Composable
private fun DebugOverlay(
    state: PlaybackDebugState,
    muted: Boolean,
    onToggleMuted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = "id: ${state.videoId ?: "-"}", color = Color.White)
        Text(text = "startup: ${state.startupTimeMs?.let { "$it ms" } ?: "..."}", color = Color.White)
        Text(text = "rebuffer: ${state.rebufferCount}", color = Color.White)
        Text(text = "bitrate: ${state.bitrateKbps?.let { "$it kbps" } ?: "-"}", color = Color.White)
        Text(text = "resolution: ${state.resolution ?: "-"}", color = Color.White)
        Text(text = "playing: ${state.isPlaying}", color = Color.White)
        Row(modifier = Modifier.padding(top = 6.dp).clickable(onClick = onToggleMuted)) {
            Text(text = if (muted) "UNMUTE" else "MUTE", color = Color.Cyan)
        }
    }
}
