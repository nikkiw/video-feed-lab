package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

internal class DesktopPagerNavigation(
    private val pagerState: PagerState,
    private val itemIndices: IntRange,
    private val scope: CoroutineScope,
    private val onScrollStart: () -> Unit,
    private val onTargetPage: (Int) -> Unit,
) {
    private val wheelEvents = Channel<Double>(capacity = Channel.UNLIMITED)

    fun moveBy(delta: Int) {
        val target = (pagerState.settledPage + delta).coerceIn(itemIndices)
        if (target != pagerState.settledPage) {
            onScrollStart()
            onTargetPage(target)
            scope.launch {
                pagerState.animateScrollToPage(
                    page = target,
                    animationSpec = tween(BUTTON_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    fun onWheel(rotation: Double) {
        if (rotation == 0.0) return
        val target = wheelTargetPage(pagerState.settledPage, rotation, itemIndices)
        if (target != pagerState.settledPage) onTargetPage(target)
        wheelEvents.trySend(rotation)
    }

    suspend fun consumeWheelGestures() {
        while (true) {
            val firstRotation = wheelEvents.receive()
            val startPage = pagerState.settledPage
            var totalRotation = 0.0
            var appliedProgress = 0f

            onScrollStart()
            pagerState.scroll(MutatePriority.UserInput) {
                var rotation: Double? = firstRotation
                while (rotation != null) {
                    totalRotation += rotation
                    val progress = wheelDragProgress(totalRotation)
                    val pageSize = pagerState.layoutInfo.pageSize
                    if (pageSize > 0) {
                        scrollBy((progress - appliedProgress) * pageSize)
                        appliedProgress = progress
                    }
                    rotation =
                        withTimeoutOrNull(WHEEL_GESTURE_IDLE_MILLIS) {
                            wheelEvents.receive()
                        }
                }
            }

            val target = wheelTargetPage(startPage, totalRotation, itemIndices)
            pagerState.animateScrollToPage(
                page = target,
                animationSpec = tween(WHEEL_SNAP_MILLIS, easing = FastOutSlowInEasing),
            )
        }
    }
}

@Composable
internal fun rememberDesktopPagerNavigation(
    pagerState: PagerState,
    itemIndices: IntRange,
    onScrollStart: () -> Unit,
    onTargetPage: (Int) -> Unit,
): DesktopPagerNavigation {
    val scope = rememberCoroutineScope()
    val currentOnScrollStart = rememberUpdatedState(onScrollStart)
    val currentOnTargetPage = rememberUpdatedState(onTargetPage)
    val navigation =
        remember(pagerState, itemIndices) {
            DesktopPagerNavigation(
                pagerState = pagerState,
                itemIndices = itemIndices,
                scope = scope,
                onScrollStart = { currentOnScrollStart.value() },
                onTargetPage = { currentOnTargetPage.value(it) },
            )
        }
    LaunchedEffect(navigation) {
        navigation.consumeWheelGestures()
    }
    return navigation
}

internal fun wheelDragProgress(totalRotation: Double): Float =
    (totalRotation * WHEEL_PAGE_FRACTION).toFloat().coerceIn(-MAX_WHEEL_DRAG, MAX_WHEEL_DRAG)

internal fun wheelTargetPage(
    startPage: Int,
    totalRotation: Double,
    itemIndices: IntRange,
): Int {
    if (abs(totalRotation) < MIN_WHEEL_ROTATION) return startPage
    val direction = totalRotation.compareTo(0.0)
    return (startPage + direction).coerceIn(itemIndices)
}

private const val WHEEL_GESTURE_IDLE_MILLIS = 110L
private const val WHEEL_SNAP_MILLIS = 280
private const val BUTTON_ANIMATION_MILLIS = 520
private const val WHEEL_PAGE_FRACTION = 0.22
private const val MAX_WHEEL_DRAG = 0.9f
private const val MIN_WHEEL_ROTATION = 0.1
