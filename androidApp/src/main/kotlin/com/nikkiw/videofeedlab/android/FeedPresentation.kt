package com.nikkiw.videofeedlab.android

import com.nikkiw.videofeedlab.feature.videofeed.api.FeedPresentationId
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedPresentations

internal enum class FeedPresentationStatus {
    Stable,
    Experimental,
    Planned,
}

internal data class FeedPresentationDescriptor(
    val id: FeedPresentationId,
    val title: String,
    val description: String,
    val status: FeedPresentationStatus,
    val isAvailable: Boolean,
)

internal object AndroidFeedPresentations {
    val VerticalSnap = FeedPresentations.VerticalSnap
    val HorizontalPageTurn = FeedPresentations.HorizontalPageTurn

    val all: List<FeedPresentationDescriptor> =
        listOf(
            FeedPresentationDescriptor(
                id = VerticalSnap,
                title = "Vertical Snap",
                description =
                    "The current full-height video feed with vertical snapping. " +
                        "Includes caching and preloading.",
                status = FeedPresentationStatus.Stable,
                isAvailable = true,
            ),
            FeedPresentationDescriptor(
                id = HorizontalPageTurn,
                title = "Horizontal Page Turn",
                description =
                    "A centered video feed with horizontal page-turn transitions. " +
                        "Currently planned for development.",
                status = FeedPresentationStatus.Planned,
                isAvailable = false,
            ),
        )

    fun find(id: FeedPresentationId): FeedPresentationDescriptor? = all.firstOrNull { it.id == id }
}
