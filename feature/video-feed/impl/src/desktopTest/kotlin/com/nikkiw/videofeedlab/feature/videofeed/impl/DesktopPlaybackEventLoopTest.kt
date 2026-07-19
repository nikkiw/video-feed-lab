package com.nikkiw.videofeedlab.feature.videofeed.impl

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopPlaybackEventLoopTest {
    @Test
    fun executesAcceptedTasksInOrderOnOneOwnerThread() {
        val eventLoop = SingleThreadDesktopPlaybackEventLoop("desktop-playback-test")
        val completed = CountDownLatch(3)
        val values = Collections.synchronizedList(mutableListOf<Int>())
        val threadNames = Collections.synchronizedList(mutableListOf<String>())

        repeat(3) { value ->
            assertTrue(
                eventLoop.dispatch {
                    eventLoop.assertOwnerThread()
                    values += value
                    threadNames += Thread.currentThread().name
                    completed.countDown()
                },
            )
        }

        assertTrue(completed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertEquals(listOf(0, 1, 2), values)
        assertEquals(listOf("desktop-playback-test"), threadNames.distinct())

        val shutdown = CountDownLatch(1)
        assertTrue(eventLoop.shutdown(shutdown::countDown))
        assertTrue(shutdown.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    @Test
    fun shutdownRunsAfterQueuedTasksAndRejectsLateWork() {
        val eventLoop = SingleThreadDesktopPlaybackEventLoop("desktop-playback-shutdown-test")
        val releaseFirstTask = CountDownLatch(1)
        val completed = CountDownLatch(1)
        val values = Collections.synchronizedList(mutableListOf<String>())

        assertTrue(
            eventLoop.dispatch {
                releaseFirstTask.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                values += "first"
            },
        )
        assertTrue(eventLoop.dispatch { values += "second" })
        assertTrue(
            eventLoop.shutdown {
                values += "shutdown"
                completed.countDown()
            },
        )
        assertFalse(eventLoop.dispatch { values += "late" })

        releaseFirstTask.countDown()
        assertTrue(completed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertEquals(listOf("first", "second", "shutdown"), values)
    }

    @Test
    fun ownerAssertionRejectsForeignThread() {
        val eventLoop = SingleThreadDesktopPlaybackEventLoop("desktop-playback-owner-test")

        assertFailsWith<IllegalStateException> {
            eventLoop.assertOwnerThread()
        }

        val shutdown = CountDownLatch(1)
        assertTrue(eventLoop.shutdown(shutdown::countDown))
        assertTrue(shutdown.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
