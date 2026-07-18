package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

@Composable
internal fun DesktopPosterImage(
    loader: DesktopPosterLoader,
    posterUrl: String,
    fallbackUrl: String,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState(loader.cached(fallbackUrl), loader, posterUrl, fallbackUrl) {
        value = loader.load(posterUrl, fallbackUrl)
    }
    bitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxSize(),
        )
    }
}

internal class DesktopPosterLoader : AutoCloseable {
    private val client = HttpClient(OkHttp) { expectSuccess = true }
    private val cache =
        object : LinkedHashMap<String, ImageBitmap>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean {
                return size > CACHE_SIZE
            }
        }

    suspend fun load(
        posterUrl: String,
        fallbackUrl: String,
    ): ImageBitmap? =
        listOf(posterUrl, fallbackUrl)
            .distinct()
            .firstNotNullOfOrNull { url -> load(url) }

    fun cached(url: String): ImageBitmap? = synchronized(cache) { cache[url] }

    override fun close() {
        client.close()
        synchronized(cache) { cache.clear() }
    }

    private suspend fun load(url: String): ImageBitmap? {
        synchronized(cache) { cache[url] }?.let { return it }
        return runCatching {
            withContext(Dispatchers.IO) {
                val bytes = client.get(url).body<ByteArray>()
                SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        }.getOrNull()?.also { bitmap ->
            synchronized(cache) { cache[url] = bitmap }
        }
    }

    private companion object {
        const val CACHE_SIZE = 8
    }
}
