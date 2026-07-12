package com.dnrohr.eulerianmagnification

import android.content.Intent
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.analysis.ViewMode

data class ValidationLaunchOverrides(
    val mode: MagnificationMode? = null,
    val viewMode: ViewMode? = null,
    val amplification: Float? = null,
    val requestedGlPreview: Boolean? = null,
    val roiSource: RoiSource? = null,
    val manualRoi: NormalizedRect? = null,
    val cameraControlsLocked: Boolean? = null,
    val controlsExpanded: Boolean? = null,
    val cleanPreview: Boolean? = null,
    val persistSettings: Boolean = false,
) {
    val hasAnyOverride: Boolean
        get() = mode != null ||
            viewMode != null ||
            amplification != null ||
            requestedGlPreview != null ||
            roiSource != null ||
            manualRoi != null ||
            cameraControlsLocked != null ||
            controlsExpanded != null ||
            cleanPreview != null

    companion object {
        const val EXTRA_MODE = "validation.mode"
        const val EXTRA_VIEW = "validation.view"
        const val EXTRA_AMPLIFICATION = "validation.amplification"
        const val EXTRA_GL_PREVIEW = "validation.glPreview"
        const val EXTRA_ROI_SOURCE = "validation.roiSource"
        const val EXTRA_MANUAL_ROI = "validation.manualRoi"
        const val EXTRA_CAMERA_LOCK = "validation.lockAeAwb"
        const val EXTRA_CONTROLS = "validation.controls"
        const val EXTRA_CLEAN = "validation.clean"
        const val EXTRA_PERSIST = "validation.persist"

        @Suppress("DEPRECATION")
        fun fromIntent(intent: Intent?): ValidationLaunchOverrides {
            return fromMap(intent?.extras?.keySet()?.associateWith { key ->
                intent.extras?.get(key)?.toString()
            }.orEmpty())
        }

        fun fromMap(values: Map<String, String?>): ValidationLaunchOverrides {
            return ValidationLaunchOverrides(
                mode = values[EXTRA_MODE].enumValue<MagnificationMode>(),
                viewMode = values[EXTRA_VIEW].enumValue<ViewMode>(),
                amplification = values[EXTRA_AMPLIFICATION]?.toFloatOrNull()?.coerceIn(1.0f, 30.0f),
                requestedGlPreview = values[EXTRA_GL_PREVIEW].booleanValue(),
                roiSource = values[EXTRA_ROI_SOURCE].enumValue<RoiSource>(),
                manualRoi = values[EXTRA_MANUAL_ROI].manualRoiValue(),
                cameraControlsLocked = values[EXTRA_CAMERA_LOCK].booleanValue(),
                controlsExpanded = values[EXTRA_CONTROLS].booleanValue(),
                cleanPreview = values[EXTRA_CLEAN].booleanValue(),
                persistSettings = values[EXTRA_PERSIST].booleanValue() ?: false,
            )
        }

        private inline fun <reified T : Enum<T>> String?.enumValue(): T? {
            val normalized = this?.trim()?.replace(" ", "") ?: return null
            return enumValues<T>().firstOrNull { candidate ->
                candidate.name.equals(normalized, ignoreCase = true) ||
                    candidate.toString().replace(" ", "").equals(normalized, ignoreCase = true)
            }
        }

        private fun String?.booleanValue(): Boolean? {
            return when (this?.trim()?.lowercase()) {
                "true", "1", "yes", "y", "on" -> true
                "false", "0", "no", "n", "off" -> false
                else -> null
            }
        }

        private fun String?.manualRoiValue(): NormalizedRect? {
            val parts = this
                ?.split(',')
                ?.map { it.trim().toFloatOrNull() }
                ?: return null
            if (parts.size != 4 || parts.any { it == null }) return null
            val left = parts[0] ?: return null
            val top = parts[1] ?: return null
            val right = parts[2] ?: return null
            val bottom = parts[3] ?: return null
            if (left !in 0.0f..1.0f || top !in 0.0f..1.0f || right !in 0.0f..1.0f || bottom !in 0.0f..1.0f) {
                return null
            }
            if (right <= left || bottom <= top) return null
            return NormalizedRect(left, top, right, bottom)
        }
    }
}
