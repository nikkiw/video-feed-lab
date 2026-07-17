package com.nikkiw.videofeedlab.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry

/**
 * Desktop application root.
 *
 * PR1 deliberately keeps the existing [VideoFeedEntry] unchanged. The root owns only
 * top-level navigation between the presentation picker and the selected feed. A later PR
 * can replace this state holder with a Decompose ChildStack without changing Home contracts.
 */
internal class DesktopAppRoot(
    private val videoFeedEntry: VideoFeedEntry,
) {
    private var destination by mutableStateOf<Destination>(Destination.Home)

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

        destination = Destination.Feed(presentationId)
    }

    private fun showHome() {
        destination = Destination.Home
    }

    @Composable
    private fun FeedDestination(onBack: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize()) {
            videoFeedEntry.Content()

            Button(
                onClick = onBack,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
            ) {
                Text("Back to feed modes")
            }
        }
    }

    private sealed interface Destination {
        data object Home : Destination

        data class Feed(
            val presentationId: FeedPresentationId,
        ) : Destination
    }
}
