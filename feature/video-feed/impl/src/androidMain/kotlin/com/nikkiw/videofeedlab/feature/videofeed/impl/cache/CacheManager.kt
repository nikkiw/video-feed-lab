package com.nikkiw.videofeedlab.feature.videofeed.impl.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object CacheManager {
    private var cache: SimpleCache? = null
    private var dataSourceFactory: CacheDataSource.Factory? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "video_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024) // 200 MB
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }

    @Synchronized
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        if (dataSourceFactory == null) {
            val simpleCache = getCache(context)
            val upstreamFactory = DefaultHttpDataSource.Factory()
            dataSourceFactory =
                CacheDataSource.Factory()
                    .setCache(simpleCache)
                    .setUpstreamDataSourceFactory(upstreamFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }
        return dataSourceFactory!!
    }
}
