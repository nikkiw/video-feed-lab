package com.nikkiw.videofeedlab.desktop

@JvmInline
internal value class FeedPresentationId(
    val value: String,
)

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

internal object DesktopFeedPresentations {
    val VerticalSnap = FeedPresentationId("vertical-snap")
    val HorizontalPageTurn = FeedPresentationId("horizontal-page-turn")

    val all: List<FeedPresentationDescriptor> =
        listOf(
            FeedPresentationDescriptor(
                id = VerticalSnap,
                title = "Vertical Snap",
                description =
                    "The current full-height video feed with vertical snapping. " +
                        "Desktop playback is still diagnostic in PR1.",
                status = FeedPresentationStatus.Stable,
                isAvailable = true,
            ),
            FeedPresentationDescriptor(
                id = HorizontalPageTurn,
                title = "Horizontal Page Turn",
                description =
                    "A centered desktop video with horizontal page-turn transitions. " +
                        "The renderer and desktop player arrive in later PRs.",
                status = FeedPresentationStatus.Planned,
                isAvailable = false,
            ),
        )

    fun find(id: FeedPresentationId): FeedPresentationDescriptor? = all.firstOrNull { it.id == id }
}
