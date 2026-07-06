package com.dnrohr.eulerianmagnification

object ManualRoiEditState {
    fun afterManualRoiChanged(wasEditing: Boolean): Boolean = wasEditing
    fun afterDoneEditing(): Boolean = false
    fun afterClearRoi(): Boolean = false
}
