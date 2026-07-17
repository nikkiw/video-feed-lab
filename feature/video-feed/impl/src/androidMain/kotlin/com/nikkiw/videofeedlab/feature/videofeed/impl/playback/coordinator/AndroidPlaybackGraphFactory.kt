package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.coordinator

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import com.nikkiw.videofeedlab.feature.videofeed.api.PlaybackDebugState
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.cache.CacheManager
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics.AndroidPlaybackClock
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics.DefaultPlaybackMetricsTracker
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.metrics.InMemoryPlaybackAnalytics
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.Media3FeedPlayerFactory
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player.Media3PlayerPool
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.policy.AndroidPlaybackPolicyProvider
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.preload.FeedTargetPreloadStatusControl
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.preload.Media3FeedPreloader
import com.nikkiw.videofeedlab.feature.videofeed.impl.playback.view.Media3PlayerViewBinder
import com.nikkiw.videofeedlab.shared.model.StreamType
import com.nikkiw.videofeedlab.shared.model.VideoItem

@OptIn(UnstableApi::class)
internal class AndroidPlaybackGraphFactory(
    private val context: Context,
) {
    fun create(
        items: List<VideoItem>,
        onDebugState: (PlaybackDebugState) -> Unit,
    ): AndroidPlaybackCoordinator {
        val policy = AndroidPlaybackPolicyProvider(context).create()
        val mediaItems = items.map { it.toMediaItem() }
        val statusControl = FeedTargetPreloadStatusControl(policy.isLowRam)
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    policy.minBufferMs,
                    policy.maxBufferMs,
                    policy.playbackBufferMs,
                    policy.rebufferMs,
                ).build()
        val builder =
            DefaultPreloadManager.Builder(context.applicationContext, statusControl)
                .setDataSourceFactory(CacheManager.getCacheDataSourceFactory(context))
                .setLoadControl(loadControl)
        val preloader = Media3FeedPreloader(builder.build(), statusControl, mediaItems, policy)
        val playerFactory = Media3FeedPlayerFactory(builder, policy)
        val pool = Media3PlayerPool(playerFactory, policy.playerCount, preloader)
        val session = PlaybackSessionState(items)
        val metrics =
            DefaultPlaybackMetricsTracker(
                clock = AndroidPlaybackClock,
                cacheReadCounter = CacheManager,
                analytics = InMemoryPlaybackAnalytics(),
                isLowRamDevice = policy.isLowRam,
                onDebugState = onDebugState,
            )
        return AndroidPlaybackCoordinator(
            items = items,
            mediaItems = mediaItems,
            policy = policy,
            session = session,
            dependencies =
                PlaybackDependencies(
                    playerPool = pool,
                    preloader = preloader,
                    metrics = metrics,
                    viewBinder = Media3PlayerViewBinder(),
                ),
        )
    }

    private fun VideoItem.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(source.uri)
            .setMimeType(
                when (source.streamType) {
                    StreamType.HLS -> MimeTypes.APPLICATION_M3U8
                    StreamType.DASH -> MimeTypes.APPLICATION_MPD
                    StreamType.PROGRESSIVE -> MimeTypes.VIDEO_MP4
                },
            ).build()
}
