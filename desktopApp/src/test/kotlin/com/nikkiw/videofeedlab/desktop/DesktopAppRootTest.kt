package com.nikkiw.videofeedlab.desktop

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.resume
import com.nikkiw.videofeedlab.feature.videofeed.api.FeedLaunchParams
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntry
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory
import java.util.concurrent.FutureTask
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class DesktopAppRootTest {
    @Test
    fun `opening feed creates child session and back destroys it`() =
        runOnUiThread {
            val lifecycle = LifecycleRegistry()
            lifecycle.resume()

            try {
                val factory = RecordingVideoFeedEntryFactory()
                val root =
                    DesktopAppRoot(
                        componentContext = DefaultComponentContext(lifecycle),
                        videoFeedEntryFactory = factory,
                    )

                root.openPresentation(DesktopFeedPresentations.VerticalSnap)

                assertIs<DesktopAppRoot.Child.Feed>(
                    root.childStack.value.active.instance,
                )
                assertEquals(1, factory.createdFeeds.size)

                val createdFeed = factory.createdFeeds.single()
                assertFalse(createdFeed.isDestroyed)

                root.showHome()

                assertIs<DesktopAppRoot.Child.Home>(
                    root.childStack.value.active.instance,
                )
                assertTrue(createdFeed.isDestroyed)
            } finally {
                lifecycle.destroy()
            }
        }

    @Test
    fun `reopening presentation creates a new feed session`() =
        runOnUiThread {
            val lifecycle = LifecycleRegistry()
            lifecycle.resume()

            try {
                val factory = RecordingVideoFeedEntryFactory()
                val root =
                    DesktopAppRoot(
                        componentContext = DefaultComponentContext(lifecycle),
                        videoFeedEntryFactory = factory,
                    )

                root.openPresentation(DesktopFeedPresentations.VerticalSnap)

                val firstSession = factory.createdFeeds.single()

                root.showHome()
                root.openPresentation(DesktopFeedPresentations.VerticalSnap)

                assertEquals(2, factory.createdFeeds.size)

                val secondSession = factory.createdFeeds.last()

                assertTrue(firstSession.isDestroyed)
                assertFalse(secondSession.isDestroyed)
                assertNotSame(
                    firstSession.componentContext,
                    secondSession.componentContext,
                )
            } finally {
                lifecycle.destroy()
            }
        }

    @Test
    fun `launch parameters are forwarded to feed factory`() =
        runOnUiThread {
            val lifecycle = LifecycleRegistry()
            lifecycle.resume()

            try {
                val factory = RecordingVideoFeedEntryFactory()
                val root =
                    DesktopAppRoot(
                        componentContext = DefaultComponentContext(lifecycle),
                        videoFeedEntryFactory = factory,
                    )

                root.openPresentation(
                    presentationId = DesktopFeedPresentations.VerticalSnap,
                    startIndex = 3,
                )

                val createdFeed = factory.createdFeeds.single()

                assertEquals(
                    DesktopFeedPresentations.VerticalSnap,
                    createdFeed.launchParams.presentationId,
                )
                assertEquals(3, createdFeed.launchParams.startIndex)
            } finally {
                lifecycle.destroy()
            }
        }

    @Test
    fun `unavailable presentation does not create feed session`(): Unit =
        runOnUiThread {
            val lifecycle = LifecycleRegistry()
            lifecycle.resume()

            try {
                val factory = RecordingVideoFeedEntryFactory()
                val root =
                    DesktopAppRoot(
                        componentContext = DefaultComponentContext(lifecycle),
                        videoFeedEntryFactory = factory,
                    )

                root.openPresentation(DesktopFeedPresentations.HorizontalPageTurn)

                assertTrue(factory.createdFeeds.isEmpty())
                assertIs<DesktopAppRoot.Child.Home>(
                    root.childStack.value.active.instance,
                )
            } finally {
                lifecycle.destroy()
            }
        }
}

private class RecordingVideoFeedEntryFactory : VideoFeedEntryFactory {
    val createdFeeds = mutableListOf<CreatedFeed>()

    override fun create(
        componentContext: ComponentContext,
        launchParams: FeedLaunchParams,
    ): VideoFeedEntry {
        val createdFeed =
            CreatedFeed(
                componentContext = componentContext,
                launchParams = launchParams,
            )

        componentContext.lifecycle.doOnDestroy {
            createdFeed.isDestroyed = true
        }

        createdFeeds += createdFeed

        return object : VideoFeedEntry {
            @Composable
            override fun Content(onBack: (() -> Unit)?) = Unit
        }
    }
}

private data class CreatedFeed(
    val componentContext: ComponentContext,
    val launchParams: FeedLaunchParams,
    var isDestroyed: Boolean = false,
)

private fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    val task = FutureTask(block)
    SwingUtilities.invokeAndWait(task)
    return task.get()
}
