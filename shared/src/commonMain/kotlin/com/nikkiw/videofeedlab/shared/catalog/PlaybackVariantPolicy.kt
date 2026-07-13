package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.model.StreamType

data class PlaybackVariant(
    val streamType: StreamType,
    val networkProfile: MediaLabNetworkProfile,
)

fun interface PlaybackVariantPolicy {
    fun variantFor(index: Int): PlaybackVariant
}

class RoundRobinPlaybackVariantPolicy : PlaybackVariantPolicy {
    override fun variantFor(index: Int): PlaybackVariant =
        when (index.mod(VARIANT_COUNT)) {
            0 -> PlaybackVariant(StreamType.HLS, MediaLabNetworkProfile.CLEAN)
            1 -> PlaybackVariant(StreamType.DASH, MediaLabNetworkProfile.LTE)
            else -> PlaybackVariant(StreamType.PROGRESSIVE, MediaLabNetworkProfile.FLAKY)
        }

    private companion object {
        const val VARIANT_COUNT = 3
    }
}
