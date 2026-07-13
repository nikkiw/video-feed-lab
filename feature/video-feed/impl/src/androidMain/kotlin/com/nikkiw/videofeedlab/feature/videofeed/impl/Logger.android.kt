package com.nikkiw.videofeedlab.feature.videofeed.impl

import android.util.Log

internal actual fun logAnalytics(message: String) {
    Log.d("PlaybackAnalytics", message)
}
