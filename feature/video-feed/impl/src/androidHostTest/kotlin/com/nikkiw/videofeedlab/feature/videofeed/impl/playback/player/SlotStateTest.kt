package com.nikkiw.videofeedlab.feature.videofeed.impl.playback.player

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertEquals

class SlotStateTest {
    @Test
    fun bufferingSlotIsPreparingNotReady() {
        assertEquals(SlotPhase.PREPARING, slotPhaseForPlaybackState(Player.STATE_BUFFERING))
        assertEquals(
            AcquisitionReadiness.PREPARING,
            acquisitionReadiness(SlotPhase.PREPARING, firstFrameGeneration = null, assignmentGeneration = 3L),
        )
    }

    @Test
    fun firstFrameMustBelongToCurrentGeneration() {
        assertEquals(
            AcquisitionReadiness.PREPARING,
            acquisitionReadiness(SlotPhase.PREPARING, firstFrameGeneration = 2L, assignmentGeneration = 3L),
        )
        assertEquals(
            AcquisitionReadiness.FIRST_FRAME_AVAILABLE,
            acquisitionReadiness(SlotPhase.PREPARING, firstFrameGeneration = 3L, assignmentGeneration = 3L),
        )
    }

    @Test
    fun readyAndEndedSlotsAreReusable() {
        assertEquals(AcquisitionReadiness.READY, acquisitionReadiness(SlotPhase.READY, null, 1L))
        assertEquals(AcquisitionReadiness.READY, acquisitionReadiness(SlotPhase.ENDED, null, 1L))
    }
}
