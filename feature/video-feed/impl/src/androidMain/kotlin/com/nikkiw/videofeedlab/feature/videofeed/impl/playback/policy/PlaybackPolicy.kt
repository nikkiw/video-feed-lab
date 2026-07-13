package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.policy

import android.app.ActivityManager
import android.content.Context

internal data class PlaybackPolicy(
    val playerCount: Int,
    val maxVideoWidth: Int,
    val maxVideoHeight: Int,
    val backwardPreloadCount: Int,
    val forwardPreloadCount: Int,
    val minBufferMs: Int = 5_000,
    val maxBufferMs: Int = 15_000,
    val playbackBufferMs: Int = 500,
    val rebufferMs: Int = 1_000,
) {
    val isLowRam: Boolean
        get() = playerCount == 1
}

internal fun interface PlaybackPolicyProvider {
    fun create(): PlaybackPolicy
}

internal class AndroidPlaybackPolicyProvider(
    context: Context,
) : PlaybackPolicyProvider {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun create(): PlaybackPolicy =
        if (activityManager.isLowRamDevice) {
            PlaybackPolicy(
                playerCount = 1,
                maxVideoWidth = 854,
                maxVideoHeight = 480,
                backwardPreloadCount = 1,
                forwardPreloadCount = 1,
            )
        } else {
            PlaybackPolicy(
                playerCount = 2,
                maxVideoWidth = 1920,
                maxVideoHeight = 1080,
                backwardPreloadCount = 1,
                forwardPreloadCount = 2,
            )
        }
}
