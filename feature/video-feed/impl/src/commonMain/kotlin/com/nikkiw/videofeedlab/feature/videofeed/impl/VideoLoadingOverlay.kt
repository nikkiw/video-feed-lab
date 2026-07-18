package com.nikkiw.videofeedlab.feature.videofeed.impl

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
internal fun VideoLoadingOverlay(
    firstFramePresented: Boolean,
    active: Boolean,
    forcePosterVisible: Boolean = false,
    modifier: Modifier = Modifier,
    posterContent: @Composable BoxScope.() -> Unit,
) {
    val animatedPosterAlpha by animateFloatAsState(
        targetValue = if (firstFramePresented && !forcePosterVisible) 0f else 1f,
        animationSpec = tween(durationMillis = POSTER_FADE_MILLIS),
        label = "PosterAlpha",
    )
    val posterAlpha = if (forcePosterVisible) 1f else animatedPosterAlpha
    if (posterAlpha <= 0f) return

    Box(
        modifier = modifier.fillMaxSize().alpha(posterAlpha).shimmerBackground(),
        contentAlignment = Alignment.Center,
    ) {
        posterContent()
        if (active && !firstFramePresented) {
            PremiumVideoLoader()
        }
    }
}

internal fun Modifier.shimmerBackground(): Modifier =
    composed {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val progress by transition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1_500, easing = LinearEasing),
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
            val xStart = progress * size.width
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(xStart, 0f),
                        end = Offset(xStart + size.width, size.height),
                    ),
            )
        }
    }

@Composable
internal fun PremiumVideoLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loader")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1_200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation",
    )
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1_000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )
    val sweepProgress by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1_500, easing = FastOutSlowInEasing),
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
        LoaderGlow()
        LoaderRing(rotation = rotation, sweepProgress = sweepProgress)
    }
}

@Composable
private fun LoaderGlow() {
    Box(
        modifier =
            Modifier
                .size(54.dp)
                .background(
                    brush =
                        Brush.radialGradient(
                            colors = listOf(Color(0x2500F2FE), Color.Transparent),
                        ),
                    shape = CircleShape,
                ),
    )
}

@Composable
private fun LoaderRing(
    rotation: Float,
    sweepProgress: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 4.5.dp.toPx()
        val sweepBrush =
            Brush.sweepGradient(
                colors =
                    listOf(
                        Color(0xFF00F2FE),
                        Color(0xFF4FACFE),
                        Color(0xFFF355FF),
                        Color(0xFF00F2FE),
                    ),
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

private const val POSTER_FADE_MILLIS = 300
