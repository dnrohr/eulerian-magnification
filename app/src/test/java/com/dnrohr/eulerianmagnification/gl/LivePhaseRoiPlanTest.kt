package com.dnrohr.eulerianmagnification.gl

import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePhaseRoiPlanTest {
    @Test
    fun capsManualRoiTextureWhilePreservingAspect() {
        val plan = LivePhaseRoiPlan(
            surfaceSize = GlTextureSize(1080, 2400),
            roi = NormalizedRect(0.25f, 0.25f, 0.75f, 0.75f),
        )

        assertEquals(GlTextureSize(540, 1200), plan.sourcePixelSize)
        assertEquals(GlTextureSize(144, 320), plan.processingSize)
    }

    @Test
    fun keepsSmallRoiAtSourceSize() {
        val plan = LivePhaseRoiPlan(
            surfaceSize = GlTextureSize(640, 480),
            roi = NormalizedRect(0.25f, 0.25f, 0.5f, 0.5f),
        )

        assertEquals(GlTextureSize(160, 120), plan.processingSize)
    }

    @Test
    fun countsWorkingAndPhaseStateTargets() {
        val plan = LivePhaseRoiPlan(
            surfaceSize = GlTextureSize(640, 480),
            roi = NormalizedRect(0.0f, 0.0f, 1.0f, 1.0f),
            maxTextureDimension = 320,
        )

        assertEquals(8, plan.phaseStatePlan.renderTargetCount)
        assertEquals(13, plan.renderTargetCount)
        assertEquals(
            plan.processingSize.width * plan.processingSize.height * 13 * 8,
            plan.estimatedBytes,
        )
        assertTrue(plan.fitsMemoryBudget)
    }

    @Test
    fun reportsWhenRoiPlanExceedsMemoryBudget() {
        val plan = LivePhaseRoiPlan(
            surfaceSize = GlTextureSize(1920, 1080),
            roi = NormalizedRect(0.0f, 0.0f, 1.0f, 1.0f),
            maxTextureDimension = 960,
            memoryBudgetBytes = 1_000_000,
        )

        assertFalse(plan.fitsMemoryBudget)
    }

    @Test
    fun rejectsInvalidRoiAndBudgets() {
        assertThrows(IllegalArgumentException::class.java) {
            LivePhaseRoiPlan(
                surfaceSize = GlTextureSize(640, 480),
                roi = NormalizedRect(0.5f, 0.5f, 0.4f, 0.7f),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            LivePhaseRoiPlan(
                surfaceSize = GlTextureSize(640, 480),
                roi = NormalizedRect(0.0f, 0.0f, 1.0f, 1.0f),
                maxTextureDimension = 0,
            )
        }
    }

    @Test
    fun warmupStatusesExposeDebugLabels() {
        assertEquals("phase warmup: waiting for reference frame", LivePhaseWarmupStatus.Uninitialized.label)
        assertEquals("phase warmup: filling temporal state", LivePhaseWarmupStatus.Warming.label)
        assertEquals("phase ready", LivePhaseWarmupStatus.Ready.label)
    }
}
