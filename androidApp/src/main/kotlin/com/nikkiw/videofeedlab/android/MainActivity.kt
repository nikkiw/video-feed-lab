package com.nikkiw.videofeedlab.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.nikkiw.videofeedlab.feature.videofeed.impl.createVideoFeedEntry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoFeedEntry = createVideoFeedEntry(defaultComponentContext())
        setContent {
            videoFeedEntry.Content()
        }
    }
}
