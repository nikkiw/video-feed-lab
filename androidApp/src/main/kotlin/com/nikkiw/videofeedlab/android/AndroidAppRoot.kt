package com.nikkiw.videofeedlab.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedLaunchParams
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedPresentationId
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory

internal class AndroidAppRoot(
    private val componentContext: ComponentContext,
    private val videoFeedEntryFactory: VideoFeedEntryFactory,
) {
    private var destination by mutableStateOf<Destination>(Destination.Home)
    private var activeFeedEntry by mutableStateOf<VideoFeedEntry?>(null)

    private val backCallback =
        BackCallback(
            isEnabled = false,
            onBackStarted = { _ -> },
            onBackProgressed = { _ -> },
            onBackCancelled = {},
            onBack = { showHome() },
        )

    init {
        componentContext.backHandler.register(backCallback)
    }

    @Composable
    fun Content() {
        when (destination) {
            Destination.Home ->
                FeedPresentationHomeScreen(
                    presentations = AndroidFeedPresentations.all,
                    onPresentationSelected = ::openPresentation,
                )

            is Destination.Feed ->
                FeedDestination(
                    onBack = ::showHome,
                )
        }
    }

    private fun openPresentation(presentationId: FeedPresentationId) {
        val presentation = AndroidFeedPresentations.find(presentationId) ?: return
        if (!presentation.isAvailable) return

        activeFeedEntry =
            videoFeedEntryFactory.create(
                componentContext = componentContext,
                launchParams = FeedLaunchParams(presentationId = presentationId),
            )
        destination = Destination.Feed(presentationId)
        backCallback.isEnabled = true
    }

    private fun showHome() {
        activeFeedEntry = null
        destination = Destination.Home
        backCallback.isEnabled = false
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
