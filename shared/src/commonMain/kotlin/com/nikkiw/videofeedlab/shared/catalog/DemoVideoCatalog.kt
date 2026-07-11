package com.nikkiw.videofeedlab.shared.catalog

import com.nikkiw.videofeedlab.shared.model.PlaybackSource
import com.nikkiw.videofeedlab.shared.model.StreamType
import com.nikkiw.videofeedlab.shared.model.VideoItem

object DemoVideoCatalog {
    /**
     * Public test streams only. Replace these with your own vertical Mux playback IDs
     * when you want a realistic 9:16 dataset.
     */
    val items: List<VideoItem> =
        listOf(
            VideoItem(
                id = "mux-hls-vod",
                title = "Mux HLS VOD",
                subtitle = "Adaptive HLS test stream",
                source =
                    PlaybackSource(
                        uri = "https://stream.mux.com/yb2L3z3Z4IKQH02HYkf9xPToVYkOC85WA.m3u8",
                        streamType = StreamType.HLS,
                    ),
            ),
            VideoItem(
                id = "shaka-live-hls",
                title = "Shaka Live HLS",
                subtitle = "Live adaptive HLS stream",
                source =
                    PlaybackSource(
                        uri = "https://storage.googleapis.com/shaka-live-assets/player-source.m3u8",
                        streamType = StreamType.HLS,
                    ),
            ),
            VideoItem(
                id = "tears-of-steel-dash",
                title = "Tears of Steel",
                subtitle = "Adaptive DASH test stream",
                source =
                    PlaybackSource(
                        uri = "https://storage.googleapis.com/shaka-demo-assets/tos-ttml/dash.mpd",
                        streamType = StreamType.DASH,
                    ),
            ),
            VideoItem(
                id = "big-buck-bunny-dash",
                title = "Big Buck Bunny",
                subtitle = "DASH-IF multi-rate sample",
                source =
                    PlaybackSource(
                        uri = "https://dash.akamaized.net/dash264/TestCases/1c/qualcomm/2/MultiRate.mpd",
                        streamType = StreamType.DASH,
                    ),
            ),
        )
}
