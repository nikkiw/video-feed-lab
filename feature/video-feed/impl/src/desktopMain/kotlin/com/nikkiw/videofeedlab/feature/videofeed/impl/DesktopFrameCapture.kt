package com.nikkiw.videofeedlab.feature.videofeed.impl

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.awt.Component
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import org.jetbrains.skia.Image as SkiaImage

internal fun captureComponentFrame(component: Component): ImageBitmap? {
    if (component.width <= 0 || component.height <= 0 || !component.isDisplayable) return null
    return if (SwingUtilities.isEventDispatchThread()) {
        component.captureFrame()
    } else {
        val result = AtomicReference<ImageBitmap?>()
        SwingUtilities.invokeAndWait { result.set(component.captureFrame()) }
        result.get()
    }
}

private fun Component.captureFrame(): ImageBitmap? =
    runCatching {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().use { graphics -> paint(graphics) }
        image.toImageBitmap()
    }.getOrNull()

internal fun BufferedImage.toImageBitmap(): ImageBitmap {
    val bgra = toBgraBytes()
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    return SkiaImage.makeRaster(info, bgra, width * BYTES_PER_PIXEL).toComposeImageBitmap()
}

internal fun BufferedImage.toBgraBytes(): ByteArray {
    val pixels = getRGB(0, 0, width, height, null, 0, width)
    val bgra = ByteArray(pixels.size * BYTES_PER_PIXEL)
    pixels.forEachIndexed { index, argb ->
        val offset = index * BYTES_PER_PIXEL
        bgra[offset] = argb.toByte()
        bgra[offset + 1] = (argb shr 8).toByte()
        bgra[offset + 2] = (argb shr 16).toByte()
        bgra[offset + 3] = (argb ushr 24).toByte()
    }
    return bgra
}

private inline fun <T : java.awt.Graphics> T.use(block: (T) -> Unit) {
    try {
        block(this)
    } finally {
        dispose()
    }
}

private const val BYTES_PER_PIXEL = 4
