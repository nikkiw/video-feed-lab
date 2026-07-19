package com.nikkiw.videofeedlab.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedLaunchParams
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedPresentationId
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory

/**
 * Desktop application root.
 *
 * Each feed destination is created with its own Decompose child context.
 * Popping the destination destroys the associated feed lifecycle and retained Store.
 */
internal class DesktopAppRoot(
    componentContext: ComponentContext,
    private val videoFeedEntryFactory: VideoFeedEntryFactory,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<Config>()

    internal val childStack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = Config.Home,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    @Composable
    fun Content() {
        Children(stack = childStack) { child ->
            when (val instance = child.instance) {
                Child.Home ->
                    FeedPresentationHomeScreen(
                        presentations = DesktopFeedPresentations.all,
                        onPresentationSelected = ::openPresentation,
                    )

                is Child.Feed ->
                    FeedDestination(
                        entry = instance.entry,
                    )
            }
        }
    }

    internal fun openPresentation(
        presentationId: FeedPresentationId,
        startIndex: Int = 0,
    ) {
        val presentation = DesktopFeedPresentations.find(presentationId) ?: return
        if (!presentation.isAvailable) return

        navigation.pushNew(
            Config.Feed(
                presentationId = presentationId,
                startIndex = startIndex.coerceAtLeast(0),
            ),
        )
    }

    internal fun showHome() {
        navigation.pop()
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext,
    ): Child =
        when (config) {
            Config.Home -> Child.Home

            is Config.Feed -> {
                val launchParams =
                    FeedLaunchParams(
                        presentationId = config.presentationId,
                        startIndex = config.startIndex,
                    )

                Child.Feed(
                    entry =
                        videoFeedEntryFactory.create(
                            componentContext = componentContext,
                            launchParams = launchParams,
                        ),
                    launchParams = launchParams,
                )
            }
        }

    @Composable
    private fun FeedDestination(entry: VideoFeedEntry) {
        Box(modifier = Modifier.fillMaxSize()) {
            entry.Content(onBack = ::showHome)
        }
    }

    internal sealed interface Child {
        data object Home : Child

        data class Feed(
            val entry: VideoFeedEntry,
            val launchParams: FeedLaunchParams,
        ) : Child
    }

    private sealed interface Config {
        data object Home : Config

        data class Feed(
            val presentationId: FeedPresentationId,
            val startIndex: Int,
        ) : Config
    }
}
