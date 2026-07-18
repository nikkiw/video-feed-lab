package com.nikkiw.videofeedlab.feature.videofeed.impl

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopPlaybackPolicyTest {
    @Test
    fun derivesScrollDirectionFromSelection() {
        assertEquals(DesktopScrollDirection.INITIAL, scrollDirection(-1, 0))
        assertEquals(DesktopScrollDirection.FORWARD, scrollDirection(2, 3))
        assertEquals(DesktopScrollDirection.BACKWARD, scrollDirection(3, 2))
    }

    @Test
    fun preloadsInScrollDirection() {
        assertEquals(3, preferredAdjacentIndex(2, DesktopScrollDirection.FORWARD, lastIndex = 5))
        assertEquals(1, preferredAdjacentIndex(2, DesktopScrollDirection.BACKWARD, lastIndex = 5))
    }

    @Test
    fun fallsBackToAvailableNeighborAtFeedEdges() {
        assertEquals(1, preferredAdjacentIndex(0, DesktopScrollDirection.BACKWARD, lastIndex = 5))
        assertEquals(4, preferredAdjacentIndex(5, DesktopScrollDirection.FORWARD, lastIndex = 5))
        assertNull(preferredAdjacentIndex(0, DesktopScrollDirection.INITIAL, lastIndex = 0))
    }

    @Test
    fun convertsWheelRotationToBoundedDrag() {
        assertEquals(0.22f, wheelDragProgress(1.0), absoluteTolerance = 0.001f)
        assertEquals(-0.22f, wheelDragProgress(-1.0), absoluteTolerance = 0.001f)
        assertEquals(0.9f, wheelDragProgress(20.0), absoluteTolerance = 0.001f)
        assertEquals(-0.9f, wheelDragProgress(-20.0), absoluteTolerance = 0.001f)
    }

    @Test
    fun snapsWheelGestureToOneAdjacentPage() {
        val indices = 0..5

        assertEquals(3, wheelTargetPage(startPage = 2, totalRotation = 1.0, indices))
        assertEquals(1, wheelTargetPage(startPage = 2, totalRotation = -1.0, indices))
        assertEquals(0, wheelTargetPage(startPage = 0, totalRotation = -1.0, indices))
        assertEquals(5, wheelTargetPage(startPage = 5, totalRotation = 1.0, indices))
        assertEquals(2, wheelTargetPage(startPage = 2, totalRotation = 0.01, indices))
    }

    @Test
    fun convertsCapturedAwtPixelsFromArgbToBgra() {
        val source = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        source.setRGB(0, 0, 0x7F123456)

        val pixels = source.toBgraBytes()

        assertContentEquals(byteArrayOf(0x56, 0x34, 0x12, 0x7F), pixels)
    }
}
