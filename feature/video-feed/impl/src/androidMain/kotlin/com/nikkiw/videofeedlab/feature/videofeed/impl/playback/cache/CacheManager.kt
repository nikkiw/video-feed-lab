package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

internal fun interface CacheReadCounter {
    fun bytesRead(): Long
}

@OptIn(UnstableApi::class)
internal object CacheManager : CacheReadCounter {
    private var cache: SimpleCache? = null
    private var dataSourceFactory: CacheDataSource.Factory? = null

    @Volatile
    private var cachedBytesRead: Long = 0L

    override fun bytesRead(): Long = cachedBytesRead

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "video_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(300L * 1024L * 1024L)
            cache = SimpleCache(cacheDir, evictor, StandaloneDatabaseProvider(context))
        }
        return requireNotNull(cache)
    }

    @Synchronized
    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        if (dataSourceFactory == null) {
            dataSourceFactory =
                CacheDataSource.Factory()
                    .setCache(getCache(context))
                    .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    .setEventListener(
                        object : CacheDataSource.EventListener {
                            override fun onCachedBytesRead(
                                cacheSizeBytes: Long,
                                cachedBytesRead: Long,
                            ) {
                                this@CacheManager.cachedBytesRead += cachedBytesRead
                            }

                            override fun onCacheIgnored(reason: Int) = Unit
                        },
                    )
        }
        return requireNotNull(dataSourceFactory)
    }
}
