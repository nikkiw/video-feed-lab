package com.nikkiw.videofeedlab.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedLaunchParams
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedPresentationId
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory

/**
 * Desktop application root.
 *
 * PR1 deliberately keeps the existing [VideoFeedEntry] unchanged. The root owns only
 * top-level navigation between the presentation picker and the selected feed. A later PR
 * can replace this state holder with a Decompose ChildStack without changing Home contracts.
 */
internal class DesktopAppRoot(
    private val componentContext: ComponentContext,
    private val videoFeedEntryFactory: VideoFeedEntryFactory,
) {
    private var destination by mutableStateOf<Destination>(Destination.Home)
    private var activeFeedEntry by mutableStateOf<VideoFeedEntry?>(null)

    @Composable
    fun Content() {
        when (destination) {
            Destination.Home ->
                FeedPresentationHomeScreen(
                    presentations = DesktopFeedPresentations.all,
                    onPresentationSelected = ::openPresentation,
                )

            is Destination.Feed ->
                FeedDestination(
                    onBack = ::showHome,
                )
        }
    }

    private fun openPresentation(presentationId: FeedPresentationId) {
        val presentation = DesktopFeedPresentations.find(presentationId) ?: return
        if (!presentation.isAvailable) return

        activeFeedEntry =
            videoFeedEntryFactory.create(
                componentContext = componentContext,
                launchParams = FeedLaunchParams(presentationId = presentationId),
            )
        destination = Destination.Feed(presentationId)
    }

    private fun showHome() {
        activeFeedEntry = null
        destination = Destination.Home
    }

    @Composable
    private fun FeedDestination(onBack: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            checkNotNull(activeFeedEntry).Content(onBack = onBack)
        }
    }

    private sealed interface Destination {
        data object Home : Destination

        data class Feed(
            val presentationId: FeedPresentationId,
        ) : Destination
    }
}
