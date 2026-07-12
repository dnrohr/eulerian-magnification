package com.dnrohr.eulerianmagnification

import android.content.Context
import android.content.SharedPreferences
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.ViewMode

data class PersistedAppSettings(
    val analysisSettings: AnalysisSettings,
    val requestedGlPreview: Boolean = false,
    val cameraControlsLocked: Boolean = false,
    val qualityCuesEnabled: Boolean = false,
) {
    fun toMap(): Map<String, String> {
        return mapOf(
            KEY_MODE to analysisSettings.mode.name,
            KEY_VIEW_MODE to analysisSettings.viewMode.name,
            KEY_AMPLIFICATION to analysisSettings.amplification.toString(),
            KEY_REQUESTED_GL_PREVIEW to requestedGlPreview.toString(),
            KEY_CAMERA_CONTROLS_LOCKED to cameraControlsLocked.toString(),
            KEY_QUALITY_CUES_ENABLED to qualityCuesEnabled.toString(),
        )
    }

    companion object {
        fun defaultFor(
            availableModes: List<MagnificationMode> = MagnificationMode.entries,
        ): PersistedAppSettings {
            val defaultMode = defaultModeFor(availableModes)
            return PersistedAppSettings(
                analysisSettings = AnalysisSettings(mode = defaultMode),
                requestedGlPreview = defaultMode.prefersGlPreviewByDefault(),
            )
        }

        fun fromMap(
            values: Map<String, String>,
            availableModes: List<MagnificationMode> = MagnificationMode.entries,
        ): PersistedAppSettings {
            val defaults = defaultFor(availableModes)
            val mode = values[KEY_MODE]
                ?.let { stored -> MagnificationMode.entries.firstOrNull { it.name == stored } }
                ?.takeIf { it in availableModes }
                ?: defaults.analysisSettings.mode
            val viewMode = values[KEY_VIEW_MODE]
                ?.let { stored -> ViewMode.entries.firstOrNull { it.name == stored } }
                ?: defaults.analysisSettings.viewMode
            val amplification = values[KEY_AMPLIFICATION]
                ?.toFloatOrNull()
                ?.coerceIn(MIN_AMPLIFICATION, MAX_AMPLIFICATION)
                ?: defaults.analysisSettings.amplification
            return PersistedAppSettings(
                analysisSettings = AnalysisSettings(
                    mode = mode,
                    viewMode = viewMode,
                    amplification = amplification,
                ),
                requestedGlPreview = values[KEY_REQUESTED_GL_PREVIEW]?.toBooleanStrictOrNull()
                    ?: defaults.requestedGlPreview,
                cameraControlsLocked = values[KEY_CAMERA_CONTROLS_LOCKED]?.toBooleanStrictOrNull() ?: false,
                qualityCuesEnabled = values[KEY_QUALITY_CUES_ENABLED]?.toBooleanStrictOrNull() ?: false,
            )
        }

        private fun defaultModeFor(availableModes: List<MagnificationMode>): MagnificationMode {
            val preferredOrder = listOf(
                MagnificationMode.ObjectVibration,
                MagnificationMode.Tremor,
                MagnificationMode.Pulse,
                MagnificationMode.Breathing,
            )
            return preferredOrder.firstOrNull { it in availableModes }
                ?: availableModes.firstOrNull()
                ?: MagnificationMode.ObjectVibration
        }

        private fun MagnificationMode.prefersGlPreviewByDefault(): Boolean {
            return this == MagnificationMode.ObjectVibration || this == MagnificationMode.Tremor
        }

        const val KEY_MODE = "mode"
        const val KEY_VIEW_MODE = "viewMode"
        const val KEY_AMPLIFICATION = "amplification"
        const val KEY_REQUESTED_GL_PREVIEW = "requestedGlPreview"
        const val KEY_CAMERA_CONTROLS_LOCKED = "cameraControlsLocked"
        const val KEY_QUALITY_CUES_ENABLED = "qualityCuesEnabled"
        private const val MIN_AMPLIFICATION = 1.0f
        private const val MAX_AMPLIFICATION = 30.0f
    }
}

class AppSettingsStore(
    context: Context,
) {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(
        availableModes: List<MagnificationMode>,
    ): PersistedAppSettings {
        val values = PersistedAppSettings.Companion.run {
            mapOf(
                KEY_MODE to preferences.getString(KEY_MODE, null),
                KEY_VIEW_MODE to preferences.getString(KEY_VIEW_MODE, null),
                KEY_AMPLIFICATION to preferences.getString(KEY_AMPLIFICATION, null),
                KEY_REQUESTED_GL_PREVIEW to preferences.getString(KEY_REQUESTED_GL_PREVIEW, null),
                KEY_CAMERA_CONTROLS_LOCKED to preferences.getString(KEY_CAMERA_CONTROLS_LOCKED, null),
                KEY_QUALITY_CUES_ENABLED to preferences.getString(KEY_QUALITY_CUES_ENABLED, null),
            )
        }.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        return PersistedAppSettings.fromMap(values, availableModes)
    }

    fun save(settings: PersistedAppSettings) {
        preferences.edit().apply {
            settings.toMap().forEach { (key, value) ->
                putString(key, value)
            }
        }.apply()
    }

    fun reset() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "app-settings"
    }
}
