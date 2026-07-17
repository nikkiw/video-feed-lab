package com.nikkiw.videofeedlab.feature.videofeed.api

import kotlin.jvm.JvmInline

/**
 * Stable identifier of a feed presentation.
 *
 * String-backed IDs allow adding new presentation implementations without
 * changing persisted navigation data or extending a central enum.
 */
@JvmInline
value class FeedPresentationId(
    val value: String,
)

/** Built-in feed presentations known by the application. */
object FeedPresentations {
    val VerticalSnap = FeedPresentationId("vertical-snap")
    val HorizontalPageTurn = FeedPresentationId("horizontal-page-turn")
}

/** Parameters used to create an independent video-feed session. */
data class FeedLaunchParams(
    val presentationId: FeedPresentationId = FeedPresentations.VerticalSnap,
    val startIndex: Int = 0,
)
