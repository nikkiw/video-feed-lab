package com.nikkiw.videofeedlab.android

import android.app.Application
import com.nikkiw.videofeedlab.feature.videofeed.impl.videoFeedModule
import com.nikkiw.videofeedlab.shared.catalog.catalogModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class VideoFeedLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@VideoFeedLabApplication)
            modules(catalogModule, videoFeedModule)
        }
    }
}
