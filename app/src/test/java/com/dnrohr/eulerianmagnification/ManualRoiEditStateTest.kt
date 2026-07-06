package com.dnrohr.eulerianmagnification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualRoiEditStateTest {
    @Test
    fun changingRoiKeepsEditModeActive() {
        assertTrue(ManualRoiEditState.afterManualRoiChanged(wasEditing = true))
    }

    @Test
    fun doneEditingLeavesEditMode() {
        assertFalse(ManualRoiEditState.afterDoneEditing())
    }

    @Test
    fun clearingRoiLeavesEditMode() {
        assertFalse(ManualRoiEditState.afterClearRoi())
    }
}
