package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualRoiSelectorTest {
    @Test
    fun normalizesDragRectangle() {
        val roi = ManualRoiSelector.fromDrag(
            startX = 20.0f,
            startY = 30.0f,
            endX = 80.0f,
            endY = 90.0f,
            width = 100.0f,
            height = 120.0f,
        )

        assertEquals(0.2f, roi?.left ?: 0.0f, 0.0f)
        assertEquals(0.25f, roi?.top ?: 0.0f, 0.0f)
        assertEquals(0.8f, roi?.right ?: 0.0f, 0.0f)
        assertEquals(0.75f, roi?.bottom ?: 0.0f, 0.0f)
    }

    @Test
    fun handlesReverseDragAndClampsToBounds() {
        val roi = ManualRoiSelector.fromDrag(
            startX = 120.0f,
            startY = -20.0f,
            endX = 50.0f,
            endY = 50.0f,
            width = 100.0f,
            height = 100.0f,
        )

        assertEquals(0.5f, roi?.left ?: 0.0f, 0.0f)
        assertEquals(0.0f, roi?.top ?: 0.0f, 0.0f)
        assertEquals(1.0f, roi?.right ?: 0.0f, 0.0f)
        assertEquals(0.5f, roi?.bottom ?: 0.0f, 0.0f)
    }

    @Test
    fun ignoresTinySelections() {
        val roi = ManualRoiSelector.fromDrag(
            startX = 10.0f,
            startY = 10.0f,
            endX = 11.0f,
            endY = 11.0f,
            width = 100.0f,
            height = 100.0f,
        )

        assertNull(roi)
    }
}
