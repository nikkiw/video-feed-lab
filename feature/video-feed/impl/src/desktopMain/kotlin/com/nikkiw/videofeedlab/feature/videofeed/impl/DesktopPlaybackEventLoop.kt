package com.nikkiw.videofeedlab.feature.videofeed.impl

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Serial owner for all mutable desktop playback state. */
internal interface DesktopPlaybackEventLoop {
    /** Queues [task] while the loop is accepting work. */
    fun dispatch(task: () -> Unit): Boolean

    /** Stops accepting new work and runs [finalTask] after already accepted tasks. */
    fun shutdown(finalTask: () -> Unit): Boolean

    /** Fails fast when mutable coordinator state is touched from another thread. */
    fun assertOwnerThread()
}

internal class SingleThreadDesktopPlaybackEventLoop(
    threadName: String = DEFAULT_THREAD_NAME,
) : DesktopPlaybackEventLoop {
    private val submissionLock = Any()
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, threadName).apply { isDaemon = true }
        }

    @Volatile
    private var ownerThread: Thread? = null

    private var acceptingTasks = true

    override fun dispatch(task: () -> Unit): Boolean =
        synchronized(submissionLock) {
            if (!acceptingTasks) return@synchronized false
            submit(task)
            true
        }

    override fun shutdown(finalTask: () -> Unit): Boolean =
        synchronized(submissionLock) {
            if (!acceptingTasks) return@synchronized false
            acceptingTasks = false
            submit {
                try {
                    finalTask()
                } finally {
                    executor.shutdown()
                }
            }
            true
        }

    override fun assertOwnerThread() {
        check(ownerThread === Thread.currentThread()) {
            "Desktop playback state must be accessed from its event-loop thread"
        }
    }

    private fun submit(task: () -> Unit) {
        executor.execute {
            ownerThread = Thread.currentThread()
            runCatching(task).onFailure(DesktopPlaybackTrace::failure)
        }
    }

    private companion object {
        const val DEFAULT_THREAD_NAME = "desktop-playback-state"
    }
}
