package com.nikkiw.videofeedlab.feature.videofeed.impl

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.nikkiw.videofeedlab.feature.videofeed.api.CatalogLoadState
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedComponent
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.coordinator.AndroidPlaybackCoordinator
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.coordinator.AndroidPlaybackGraphFactory
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.coordinator.PagePlaybackState
import com.nikkiw.videofeedlab.shared.model.VideoItem
import android.graphics.Color as AndroidColor

private data class VideoPageUi(
    val item: VideoItem,
    val playback: PagePlaybackState,
    val active: Boolean,
    val page: Int,
)

@Composable
internal actual fun PlatformVideoFeedScreen(component: VideoFeedComponent) {
    val model by component.models.subscribeAsState()
    when (val loadState = model.catalogLoadState) {
        CatalogLoadState.Content -> VideoFeedContent(component, model)
        CatalogLoadState.Loading -> CatalogStatusScreen(message = "Loading video catalog…", showProgress = true)
        CatalogLoadState.Empty -> CatalogStatusScreen(message = "No videos found")
        is CatalogLoadState.Error -> CatalogStatusScreen(message = loadState.message, onRetry = component::onRetryLoad)
    }
}

@Composable
private fun VideoFeedContent(
    component: VideoFeedComponent,
    model: VideoFeedComponent.Model,
) {
    val context = LocalContext.current
    val coordinator =
        remember(model.items) {
            AndroidPlaybackGraphFactory(context).create(
                items = model.items,
                onDebugState = { debug ->
                    (component as? DefaultVideoFeedComponent)?.updateDebugInfo(debug)
                },
            )
        }

    val pagerState = rememberPagerState(pageCount = { model.items.size })
    val pages by coordinator.pages.collectAsState()

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

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            coordinator.onScrollStart()
        }
    }

    LaunchedEffect(pagerState.targetPage, coordinator) {
        val targetPage = pagerState.targetPage
        if (targetPage != pagerState.settledPage && targetPage in model.items.indices) {
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
                ui =
                    VideoPageUi(
                        item = model.items[page],
                        playback = pages.getValue(page),
                        active = page == pagerState.settledPage,
                        page = page,
                    ),
                onTogglePlay = {
                    coordinator.togglePlayPause()
                    component.onTogglePlay()
                },
                coordinator = coordinator,
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
private fun CatalogStatusScreen(
    message: String,
    showProgress: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showProgress) CircularProgressIndicator(color = Color.White)
            Text(text = message, color = Color.White)
            onRetry?.let { retry ->
                Button(onClick = retry) {
                    Text("Retry")
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoFeedItemView(
    ui: VideoPageUi,
    onTogglePlay: () -> Unit,
    coordinator: AndroidPlaybackCoordinator,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(ui.active) {
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
                    setKeepContentOnPlayerReset(true) // Keep content on reset (Stage 1.3)
                    setShutterBackgroundColor(AndroidColor.TRANSPARENT) // Transparent shutter (Stage 1.4)
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
                // Bind player via coordinator using switchTargetView (Stage 1.2)
                coordinator.bindPlayerView(ui.page, playerView, ui.playback)
            },
            onRelease = { playerView -> coordinator.unbindPlayerView(ui.page, playerView) },
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = false }
                    .focusable(false),
        )

        VideoPosterOverlay(playback = ui.playback, active = ui.active)

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = ui.item.title, color = Color.White)
            Text(text = ui.item.subtitle, color = Color.LightGray)
            Text(text = ui.item.source.streamType.name, color = Color.LightGray)
        }
    }
}

fun Modifier.shimmerBackground(): Modifier =
    composed {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val progress by transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmerProgress",
        )

        val shimmerColors =
            listOf(
                Color(0xFF16161A),
                Color(0xFF2A2A32),
                Color(0xFF16161A),
            )

        drawBehind {
            val width = size.width
            val height = size.height
            val xStart = progress * width
            val xEnd = xStart + width

            val brush =
                Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(xStart, 0f),
                    end = Offset(xEnd, height),
                )
            drawRect(brush = brush)
        }
    }

@Composable
private fun PremiumVideoLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loader")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation",
    )

    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )

    val sweepProgress by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "sweep",
    )

    Box(
        modifier =
            modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(54.dp)
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color(0x2500F2FE),
                                        Color(0x00000000),
                                    ),
                            ),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.5.dp.toPx()
            val gradientColors =
                listOf(
                    Color(0xFF00F2FE),
                    Color(0xFF4FACFE),
                    Color(0xFFF355FF),
                    Color(0xFF00F2FE),
                )

            val sweepBrush =
                Brush.sweepGradient(
                    colors = gradientColors,
                    center = center,
                )

            drawCircle(
                color = Color(0x1AFFFFFF),
                radius = (size.minDimension - strokeWidth) / 2,
                style = Stroke(width = strokeWidth),
            )

            rotate(degrees = rotation) {
                drawArc(
                    brush = sweepBrush,
                    startAngle = 0f,
                    sweepAngle = sweepProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun VideoPosterOverlay(
    playback: PagePlaybackState,
    active: Boolean,
) {
    val posterAlpha by animateFloatAsState(
        targetValue = if (playback.firstFrameRendered) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "PosterAlpha",
    )
    if (posterAlpha <= 0f) return
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(posterAlpha)
                .shimmerBackground(),
        contentAlignment = Alignment.Center,
    ) {
        playback.lqip?.let { lqip ->
            AsyncImage(
                model = lqip,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        playback.placeholderUrl?.let { placeholderUrl ->
            AsyncImage(
                model = placeholderUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        AsyncImage(
            model = playback.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (active && !playback.firstFrameRendered) {
            PremiumVideoLoader()
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
        Text(text = "low-ram: ${state.isLowRamDevice}", color = Color.White)
        Text(text = "id: ${state.videoId ?: "-"}", color = Color.White)
        Text(text = "bitrate: ${state.bitrateKbps?.let { "$it kbps" } ?: "-"}", color = Color.White)
        Text(text = "resolution: ${state.resolution ?: "-"}", color = Color.White)
        Text(text = "playing: ${state.isPlaying}", color = Color.White)
        Text(text = "source: ${state.startupSource ?: "-"}", color = Color.White)
        Text(text = "direction: ${state.startupDirection ?: "-"}", color = Color.White)
        Text(text = "cache read: ${state.cacheRead}", color = Color.White)

        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "ENTERPRISE ANALYTICS", color = Color.Green)
        Text(text = "starts: ${state.videoStarts}", color = Color.White)
        Text(text = "median (p50): ${state.p50StartupTimeMs} ms", color = Color.White)
        Text(text = "p95: ${state.p95StartupTimeMs} ms (n=${state.sampleCount})", color = Color.White)
        Text(text = "rebuffers: ${state.totalRebuffers}", color = Color.White)
        Text(text = "errors: ${state.totalErrors}", color = Color.White)
        Text(text = "dropped: ${state.totalDroppedFrames}", color = Color.White)

        Row(modifier = Modifier.padding(top = 8.dp).clickable(onClick = onToggleMuted)) {
            Text(text = if (muted) "UNMUTE" else "MUTE", color = Color.Cyan)
        }
    }
}
