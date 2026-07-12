package com.nikkiw.videofeedlab.feature.videofeed.impl

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
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

    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction, coordinator) {
        val offset = pagerState.currentPageOffsetFraction
        val current = pagerState.currentPage
        val targetPage =
            when {
                offset > 0.05f -> current + 1
                offset < -0.05f -> current - 1
                else -> null
            }
        if (targetPage != null && targetPage in 0 until model.items.size) {
            coordinator.preloadPage(targetPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            VideoFeedItemView(
                item = model.items[page],
                player = coordinator.getPlayerForIndex(page),
                isFirstFrameRendered = coordinator.isFirstFrameRenderedForIndex(page),
                active = page == pagerState.settledPage,
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
    player: ExoPlayer?,
    isFirstFrameRendered: Boolean,
    active: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(active) {
                    detectTapGestures(onTap = { onTogglePlay() })
                }
                .focusProperties { canFocus = false }
                .focusable(false),
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
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                }
            },
            update = { playerView ->
                playerView.player = player
            },
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = false }
                    .focusable(false),
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
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = "SYSTEM & PLAYER", color = Color.Yellow)
        Text(text = "low-ram: ${AnalyticsManager.isLowRamDevice}", color = Color.White)
        Text(text = "id: ${state.videoId ?: "-"}", color = Color.White)
        Text(text = "bitrate: ${state.bitrateKbps?.let { "$it kbps" } ?: "-"}", color = Color.White)
        Text(text = "resolution: ${state.resolution ?: "-"}", color = Color.White)
        Text(text = "playing: ${state.isPlaying}", color = Color.White)

        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "ENTERPRISE ANALYTICS", color = Color.Green)
        Text(text = "starts: ${AnalyticsManager.videoStarts}", color = Color.White)
        Text(text = "median (p50): ${AnalyticsManager.p50StartupTime} ms", color = Color.White)
        Text(text = "cold (p95): ${AnalyticsManager.p95StartupTime} ms", color = Color.White)
        Text(text = "rebuffers: ${AnalyticsManager.totalRebuffers}", color = Color.White)
        Text(text = "errors: ${AnalyticsManager.totalErrors}", color = Color.White)

        Row(modifier = Modifier.padding(top = 8.dp).clickable(onClick = onToggleMuted)) {
            Text(text = if (muted) "UNMUTE" else "MUTE", color = Color.Cyan)
        }
    }
}
