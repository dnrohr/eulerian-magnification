package com.dnrohr.eulerianmagnification.gl

import org.junit.Assert.assertEquals
import org.junit.Test

class GlTemporalStateTest {
    @Test
    fun layoutCopiesPyramidLevelSizes() {
        val sizes = GlPyramid.pyramidSizes(
            baseSize = GlTextureSize(320, 180),
            levelCount = 3,
        )

        assertEquals(
            listOf(
                GlTextureSize(320, 180),
                GlTextureSize(160, 90),
                GlTextureSize(80, 45),
            ),
            sizes,
        )
    }

    @Test
    fun temporalPlanHasTwoTargetsPerLevel() {
        val plan = TemporalStatePlan(
            levelSizes = listOf(
                GlTextureSize(320, 180),
                GlTextureSize(160, 90),
            ),
        )

        assertEquals(4, plan.renderTargetCount)
    }
}

data class TemporalStatePlan(
    val levelSizes: List<GlTextureSize>,
) {
    val renderTargetCount: Int get() = levelSizes.size * 2
}
