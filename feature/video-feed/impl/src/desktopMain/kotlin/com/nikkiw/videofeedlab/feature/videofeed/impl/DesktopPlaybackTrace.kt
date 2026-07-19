package com.nikkiw.videofeedlab.feature.videofeed.impl

import java.util.logging.Level
import java.util.logging.Logger

/** Opt-in desktop playback tracing kept out of the measured hot path by default. */
internal object DesktopPlaybackTrace {
    private const val SYSTEM_PROPERTY = "videofeedlab.desktop.playback.trace"
    private const val ENVIRONMENT_VARIABLE = "VIDEO_FEED_LAB_DESKTOP_TRACE"

    val enabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        System.getProperty(SYSTEM_PROPERTY).toBoolean() ||
            System.getenv(ENVIRONMENT_VARIABLE).toBoolean()
    }

    private val logger: Logger by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Logger.getLogger("DesktopPlayback")
    }

    fun log(message: () -> String) {
        if (enabled) {
            logger.info(message())
        }
    }

    fun failure(error: Throwable) {
        logger.log(Level.SEVERE, "Desktop playback event-loop task failed", error)
    }
}
