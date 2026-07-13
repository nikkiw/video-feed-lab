package com.nikkiw.videofeedlab.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.nikkiw.videofeedlab.feature.videofeed.api.VideoFeedEntryFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {
    private val videoFeedEntryFactory: VideoFeedEntryFactory by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoFeedEntry = videoFeedEntryFactory.create(defaultComponentContext())
        setContent {
            videoFeedEntry.Content()
        }
    }
}
