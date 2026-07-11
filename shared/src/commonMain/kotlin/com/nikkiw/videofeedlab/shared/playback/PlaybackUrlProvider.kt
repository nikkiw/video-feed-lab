package com.nikkiw.videofeedlab.shared.playback

import com.nikkiw.videofeedlab.shared.model.PlaybackSource

/**
 * Boundary for future entitlement / signed URL resolution.
 * The demo build uses public URLs and therefore does not need a backend.
 */
fun interface PlaybackUrlProvider {
    suspend fun resolve(videoId: String): PlaybackSource
}
