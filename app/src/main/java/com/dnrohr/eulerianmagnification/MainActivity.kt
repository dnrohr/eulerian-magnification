package com.dnrohr.eulerianmagnification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.opengl.GLSurfaceView
import android.provider.OpenableColumns
import android.util.Range
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.hardware.camera2.CaptureRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.BreathingMotionFilter
import com.dnrohr.eulerianmagnification.analysis.BreathingMotionSample
import com.dnrohr.eulerianmagnification.analysis.ManualRoiSelector
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.NormalizedRect
import com.dnrohr.eulerianmagnification.analysis.PulseRoiAnalyzer
import com.dnrohr.eulerianmagnification.analysis.PreviewRoiMapper
import com.dnrohr.eulerianmagnification.analysis.PreviewRoiMappingPolicy
import com.dnrohr.eulerianmagnification.analysis.PreviewRenderPath
import com.dnrohr.eulerianmagnification.analysis.PreviewSize
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoAnalysisRunner
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoDecodeOptions
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoEvidenceReport
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoEvidenceTimeline
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoFrameDecoder
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoProcessor
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoValidationResult
import com.dnrohr.eulerianmagnification.analysis.RoiState
import com.dnrohr.eulerianmagnification.analysis.RoiSource
import com.dnrohr.eulerianmagnification.analysis.RoiSourcePolicy
import com.dnrohr.eulerianmagnification.analysis.VisualizationModel
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.capabilities.CapabilityReportStore
import com.dnrohr.eulerianmagnification.capabilities.CapabilityReporter
import com.dnrohr.eulerianmagnification.capabilities.CapabilityAvailability
import com.dnrohr.eulerianmagnification.capabilities.FeatureAvailability
import com.dnrohr.eulerianmagnification.gl.CameraOesRenderer
import com.dnrohr.eulerianmagnification.gl.ColorMagnificationParameters
import com.dnrohr.eulerianmagnification.gl.GlFrameStats
import com.dnrohr.eulerianmagnification.gl.ProcessedGlFrame
import com.dnrohr.eulerianmagnification.profiling.PerformanceBenchmark
import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor
import com.dnrohr.eulerianmagnification.quality.LightingDiagnostic
import com.dnrohr.eulerianmagnification.quality.LightingDiagnosticStatus
import com.dnrohr.eulerianmagnification.quality.LightingStabilityAnalyzer
import com.dnrohr.eulerianmagnification.quality.QualityCuePolicy
import com.dnrohr.eulerianmagnification.quality.QualityCueState
import com.dnrohr.eulerianmagnification.quality.QualityEvaluator
import com.dnrohr.eulerianmagnification.quality.QualityStatus
import com.dnrohr.eulerianmagnification.recording.DebugProcessedMp4Recorder
import com.dnrohr.eulerianmagnification.recording.GlProcessedMp4Recorder
import com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSession
import com.dnrohr.eulerianmagnification.recording.RecordingOutputKind
import com.dnrohr.eulerianmagnification.recording.RecordingOutputMode
import com.dnrohr.eulerianmagnification.recording.RecordingOutputPolicy
import com.dnrohr.eulerianmagnification.recording.RecordingRendererDiagnostics
import com.dnrohr.eulerianmagnification.recording.RecordedVideoMp4Exporter
import com.dnrohr.eulerianmagnification.recording.RecordingGallery
import com.dnrohr.eulerianmagnification.recording.RecordingGalleryItem
import com.dnrohr.eulerianmagnification.ui.AppTheme
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val capabilityReporter = CapabilityReporter(this)
        val capabilityReport = capabilityReporter.buildReport()
        capabilityReporter.logSummary()
        CapabilityReportStore(this).writeLatestReport(capabilityReporter)
        val featureAvailability = CapabilityAvailability.fromReport(capabilityReport)

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        featureAvailability = featureAvailability,
                        launchOverrides = ValidationLaunchOverrides.fromIntent(intent),
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    featureAvailability: FeatureAvailability,
    launchOverrides: ValidationLaunchOverrides = ValidationLaunchOverrides(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }
    val recordingRootDirectory = remember(context) { recordingsRoot(context) }
    val appSettingsStore = remember(context) { AppSettingsStore(context) }
    val persistedAppSettings = remember(context, featureAvailability) {
        appSettingsStore.load(featureAvailability.availableModes)
    }
    val validationExecutor = remember { Executors.newSingleThreadExecutor() }
    var analysisSample by remember { mutableStateOf(AnalysisSample()) }
    var analysisSettings by remember {
        mutableStateOf(
            persistedAppSettings.analysisSettings.copy(
                mode = launchOverrides.mode ?: persistedAppSettings.analysisSettings.mode,
                viewMode = launchOverrides.viewMode ?: persistedAppSettings.analysisSettings.viewMode,
                amplification = launchOverrides.amplification ?: persistedAppSettings.analysisSettings.amplification,
            )
        )
    }
    var recordingSession by remember { mutableStateOf<ProcessedRecordingSession?>(null) }
    var lastRecordingPath by remember { mutableStateOf<String?>(null) }
    var recentRecordings by remember { mutableStateOf(RecordingGallery.listRecent(recordingRootDirectory)) }
    var validationSummary by remember { mutableStateOf<String?>(null) }
    var validationRunning by remember { mutableStateOf(false) }
    var cameraControlsLocked by remember {
        mutableStateOf(launchOverrides.cameraControlsLocked ?: persistedAppSettings.cameraControlsLocked)
    }
    var qualityCuesEnabled by remember { mutableStateOf(persistedAppSettings.qualityCuesEnabled) }
    var recordingOutputMode by remember { mutableStateOf(persistedAppSettings.recordingOutputMode) }
    var roiSource by remember { mutableStateOf(launchOverrides.roiSource ?: persistedAppSettings.roiSource) }
    var showGlDebug by remember {
        mutableStateOf(launchOverrides.requestedGlPreview ?: persistedAppSettings.requestedGlPreview)
    }
    var controlsExpanded by remember { mutableStateOf(launchOverrides.controlsExpanded ?: false) }
    var cleanPreview by remember { mutableStateOf(launchOverrides.cleanPreview ?: false) }
    var manualRoi by remember { mutableStateOf<NormalizedRect?>(launchOverrides.manualRoi) }
    var manualRoiEditing by remember { mutableStateOf(false) }
    var glFrameStats by remember { mutableStateOf(GlFrameStats()) }
    var overlaySample by remember { mutableStateOf(AnalysisSample()) }
    var overlayBreathingMotionSample by remember { mutableStateOf(BreathingMotionSample()) }
    var overlayGlFrameStats by remember { mutableStateOf(GlFrameStats()) }
    var overlayLightingDiagnostic by remember { mutableStateOf<LightingDiagnostic?>(null) }
    var overlayQualityStatuses by remember { mutableStateOf(listOf(QualityStatus.Good)) }
    var lastOverlayTelemetryMillis by remember { mutableStateOf(0L) }
    var lastOverlayGlStatsMillis by remember { mutableStateOf(0L) }
    var fullFrameRoiFallbackState by remember { mutableStateOf(FullFrameRoiFallbackState()) }
    val qualityEvaluator = remember { QualityEvaluator() }
    val lightingStabilityAnalyzer = remember { LightingStabilityAnalyzer() }
    val artifactSuppressor = remember { ArtifactSuppressor() }
    val breathingMotionFilter = remember(analysisSettings.mode, analysisSettings.amplification) {
        BreathingMotionFilter(amplification = analysisSettings.amplification)
    }
    val usingGlPreview = PreviewPathPolicy.useGlPreview(
        settings = analysisSettings,
        requestedGlPreview = showGlDebug,
        glPreviewAvailable = featureAvailability.glPreviewAvailable,
    )
    val liveEvmPreviewDecision = LiveEvmPreviewPolicy.decide(
        settings = analysisSettings,
        usingGlPreview = usingGlPreview,
        glFrameStats = glFrameStats,
        analysisFps = analysisSample.analysisFps,
    )
    val activeRoi = RoiSourcePolicy.activeRoi(
        source = roiSource,
        autoRoi = analysisSample.roi,
        manualRoi = manualRoi,
    )
    val livePhasePreviewDecision = LivePhasePreviewPolicy.decide(
        settings = analysisSettings,
        usingGlPreview = usingGlPreview,
        glFrameStats = glFrameStats,
        surfaceSize = glFrameStats.surfaceSize,
        phaseRoi = activeRoi,
        roiSource = roiSource,
    )
    var lightingDiagnostic by remember {
        mutableStateOf<LightingDiagnostic?>(null)
    }
    var breathingMotionSample by remember { mutableStateOf(BreathingMotionSample()) }
    val overlaySignalHistory = remember { mutableStateListOf<Double>() }
    val overlayBreathingMotionHistory = remember { mutableStateListOf<Double>() }
    var previousQualityStatuses by remember { mutableStateOf(listOf(QualityStatus.Good)) }
    var qualityCueState by remember { mutableStateOf(QualityCueState()) }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            validationRunning = true
            validationSummary = "Video processing: running"
            val settings = analysisSettings
            validationExecutor.execute {
                val outcome = runCatching {
                    val displayName = displayNameForUri(context, uri)
                    val inputFile = copyUriToCacheFile(context, uri, displayName)
                    processRecordedVideoExport(
                        inputFile = inputFile,
                        sourceName = displayName ?: inputFile.name,
                        settings = settings,
                        rootDirectory = recordingRootDirectory,
                    )
                }.getOrElse { error ->
                    ProcessedVideoOutcome(
                        summary = "Video processing failed: ${error.message ?: error::class.java.simpleName}",
                        metadataFile = null,
                    )
                }
                ContextCompat.getMainExecutor(context).execute {
                    validationSummary = outcome.summary
                    outcome.metadataFile?.let { metadata ->
                        lastRecordingPath = metadata.absolutePath
                        recentRecordings = RecordingGallery.listRecent(recordingRootDirectory)
                    }
                    validationRunning = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            validationExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(analysisSettings.mode) {
        overlaySignalHistory.clear()
        overlayBreathingMotionHistory.clear()
        overlayBreathingMotionSample = BreathingMotionSample()
        if (analysisSettings.mode != MagnificationMode.Breathing) {
            breathingMotionSample = BreathingMotionSample()
        }
    }

    LaunchedEffect(featureAvailability, analysisSettings.mode, showGlDebug) {
        if (analysisSettings.mode !in featureAvailability.availableModes && featureAvailability.availableModes.isNotEmpty()) {
            analysisSettings = analysisSettings.copy(mode = featureAvailability.availableModes.first())
        }
        if (showGlDebug && !featureAvailability.glPreviewAvailable) {
            showGlDebug = false
        }
    }

    LaunchedEffect(analysisSettings, showGlDebug, cameraControlsLocked, qualityCuesEnabled, recordingOutputMode, roiSource) {
        if (!launchOverrides.hasAnyOverride || launchOverrides.persistSettings) {
            appSettingsStore.save(
                PersistedAppSettings(
                    analysisSettings = analysisSettings,
                    requestedGlPreview = showGlDebug,
                    cameraControlsLocked = cameraControlsLocked,
                    qualityCuesEnabled = qualityCuesEnabled,
                    recordingOutputMode = recordingOutputMode,
                    roiSource = roiSource,
                )
            )
        }
    }

    fun handleSample(sample: AnalysisSample): CameraSampleResult {
        analysisSample = sample
        val fullFrameFallbackDecision = FullFrameRoiFallbackPolicy.observe(
            roiSource = roiSource,
            analysisFps = sample.analysisFps,
            state = fullFrameRoiFallbackState,
        )
        fullFrameRoiFallbackState = fullFrameFallbackDecision.nextState
        if (fullFrameFallbackDecision.shouldFallbackToAuto) {
            roiSource = RoiSource.Auto
            manualRoiEditing = false
            fullFrameRoiFallbackState = FullFrameRoiFallbackState()
        }
        val currentLightingDiagnostic = lightingStabilityAnalyzer.update(sample)
        lightingDiagnostic = currentLightingDiagnostic
        val presentationTimestampNanos = recordingSession
            ?.record(sample, analysisSettings)
            ?.presentationTimestampNanos
            ?: sample.frameTimestampNanos.coerceAtLeast(0L)
        val nowMillis = SystemClock.elapsedRealtime()
        val shouldUpdateOverlayTelemetry = nowMillis - lastOverlayTelemetryMillis >= UI_TELEMETRY_INTERVAL_MILLIS
        if (analysisSettings.mode == MagnificationMode.Breathing) {
            val motion = breathingMotionFilter.update(
                translation = sample.translation,
                timestampNanos = sample.frameTimestampNanos,
            )
            breathingMotionSample = motion
            if (shouldUpdateOverlayTelemetry) {
                overlayBreathingMotionSample = motion
                overlayBreathingMotionHistory.add(motion.amplifiedDy)
                if (overlayBreathingMotionHistory.size > SIGNAL_HISTORY_SIZE) {
                    overlayBreathingMotionHistory.removeAt(0)
                }
            }
        }
        if (shouldUpdateOverlayTelemetry) {
            overlaySample = sample
            overlayLightingDiagnostic = currentLightingDiagnostic
            overlaySignalHistory.add(sample.bandpassedGreen)
            if (overlaySignalHistory.size > SIGNAL_HISTORY_SIZE) {
                overlaySignalHistory.removeAt(0)
            }
            if (analysisSettings.mode != MagnificationMode.Breathing) {
                overlayBreathingMotionSample = BreathingMotionSample()
            }
            lastOverlayTelemetryMillis = nowMillis
        }
        return CameraSampleResult(
            presentationTimestampNanos = presentationTimestampNanos,
            lightingDiagnostic = currentLightingDiagnostic,
        )
    }

    val qualityStatuses = qualityEvaluator.evaluate(
        sample = analysisSample,
        settings = analysisSettings,
        lightingFlickerLikely = lightingDiagnostic?.flickerLikely == true,
        lightingUnstable = lightingDiagnostic?.status == LightingDiagnosticStatus.ExposurePumping,
        cameraFrameFps = glFrameStats.averageFps.takeIf { usingGlPreview },
        cameraFrameSampleCount = if (usingGlPreview) glFrameStats.sampleCount else 0,
        roiSource = roiSource,
    )

    LaunchedEffect(lastOverlayTelemetryMillis) {
        overlayQualityStatuses = qualityStatuses
    }

    LaunchedEffect(qualityStatuses, qualityCuesEnabled) {
        val decision = QualityCuePolicy.decide(
            previousStatuses = previousQualityStatuses,
            currentStatuses = qualityStatuses,
            state = qualityCueState,
            nowMillis = System.currentTimeMillis(),
            enabled = qualityCuesEnabled,
            systemHapticsAllowed = view.isHapticFeedbackEnabled,
        )
        qualityCueState = decision.nextState
        previousQualityStatuses = qualityStatuses
        if (decision.shouldCue) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission && featureAvailability.liveCameraAvailable) {
            if (usingGlPreview) {
                key(analysisSettings, roiSource, manualRoi, cameraControlsLocked) {
                    CameraGlPreview(
                        settings = analysisSettings,
                        roiSource = roiSource,
                        manualRoi = manualRoi,
                        cameraControlsLocked = cameraControlsLocked,
                        liveEvmPreviewDecision = liveEvmPreviewDecision,
                        livePhasePreviewDecision = livePhasePreviewDecision,
                        onStats = { stats ->
                            val nowMillis = SystemClock.elapsedRealtime()
                            if (nowMillis - lastOverlayGlStatsMillis >= UI_TELEMETRY_INTERVAL_MILLIS) {
                                glFrameStats = stats
                                overlayGlFrameStats = stats
                                lastOverlayGlStatsMillis = nowMillis
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onSample = ::handleSample,
                        onProcessedFrame = { frame -> recordingSession?.record(frame) },
                    )
                }
            } else key(analysisSettings, roiSource, manualRoi, cameraControlsLocked) {
                CameraPreview(
                    settings = analysisSettings,
                    roiSource = roiSource,
                    manualRoi = manualRoi,
                    cameraControlsLocked = cameraControlsLocked,
                    modifier = Modifier.fillMaxSize(),
                    onSample = { sample -> handleSample(sample) },
                )
            }
            if (!liveEvmPreviewDecision.fullFrameColorPreview) {
                AmplifiedTintOverlay(
                    sample = analysisSample,
                    settings = analysisSettings,
                    artifactSuppressor = artifactSuppressor,
                    mappingPolicy = PreviewRoiMappingPolicy.frontCamera(
                        if (usingGlPreview) PreviewRenderPath.Gl else PreviewRenderPath.CameraX
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (roiSource != RoiSource.Manual) {
                RoiOverlay(
                    sample = analysisSample,
                    mappingPolicy = PreviewRoiMappingPolicy.frontCamera(
                        if (usingGlPreview) PreviewRenderPath.Gl else PreviewRenderPath.CameraX
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ManualRoiOverlay(
                roi = manualRoi.takeIf { roiSource == RoiSource.Manual },
                sample = analysisSample,
                mappingPolicy = PreviewRoiMappingPolicy.frontCamera(
                    if (usingGlPreview) PreviewRenderPath.Gl else PreviewRenderPath.CameraX
                ),
                editing = manualRoiEditing && roiSource == RoiSource.Manual,
                onRoiChanged = {
                    manualRoi = it
                    manualRoiEditing = ManualRoiEditState.afterManualRoiChanged(manualRoiEditing)
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            if (hasCameraPermission) {
                UnavailablePane(message = "This device does not report a front camera for live magnification.")
            } else {
                PermissionPane(onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                })
            }
        }

        StatusOverlay(
            sample = overlaySample,
            signalHistory = overlaySignalHistory,
            breathingMotionSample = overlayBreathingMotionSample,
            breathingMotionHistory = overlayBreathingMotionHistory,
            settings = analysisSettings,
            onSettingsChanged = { analysisSettings = it },
            cameraControlsLocked = cameraControlsLocked,
            onCameraControlsLockedChanged = { cameraControlsLocked = it },
            qualityCuesEnabled = qualityCuesEnabled,
            onQualityCuesEnabledChanged = { qualityCuesEnabled = it },
            roiSource = roiSource,
            onRoiSourceChanged = {
                roiSource = it
                manualRoiEditing = false
            },
            manualRoi = manualRoi,
            manualRoiEditing = manualRoiEditing,
            onManualRoiEditingChanged = {
                roiSource = RoiSource.Manual
                manualRoiEditing = it
            },
            onClearManualRoi = {
                manualRoi = null
                manualRoiEditing = ManualRoiEditState.afterClearRoi()
            },
            onResetSettings = {
                val defaults = PersistedAppSettings.defaultFor(featureAvailability.availableModes)
                appSettingsStore.reset()
                analysisSettings = defaults.analysisSettings
                showGlDebug = defaults.requestedGlPreview
                cameraControlsLocked = defaults.cameraControlsLocked
                qualityCuesEnabled = defaults.qualityCuesEnabled
                recordingOutputMode = defaults.recordingOutputMode
                roiSource = defaults.roiSource
                manualRoi = null
                manualRoiEditing = ManualRoiEditState.afterClearRoi()
            },
            showGlDebug = showGlDebug,
            onShowGlDebugChanged = { showGlDebug = it },
            usingGlPreview = usingGlPreview,
            featureAvailability = featureAvailability,
            glFrameStats = overlayGlFrameStats,
            liveEvmPreviewDecision = liveEvmPreviewDecision,
            livePhasePreviewDecision = livePhasePreviewDecision,
            isRecording = recordingSession != null,
            recordingElapsedMillis = recordingSession?.elapsedMillis ?: 0L,
            lastRecordingPath = lastRecordingPath,
            recentRecordings = recentRecordings,
            validationSummary = validationSummary,
            validationRunning = validationRunning,
            lightingDiagnostic = overlayLightingDiagnostic,
            qualityStatuses = overlayQualityStatuses,
            recordingOutputMode = recordingOutputMode,
            onRecordingOutputModeChanged = { recordingOutputMode = it },
            controlsExpanded = controlsExpanded,
            initialExpandedPanel = launchOverrides.expandedPanel ?: ExpandedPanelTab.Controls,
            cleanPreview = cleanPreview,
            onShowControls = {
                cleanPreview = false
                controlsExpanded = true
            },
            onHideControls = {
                controlsExpanded = false
                cleanPreview = false
            },
            onEnterCleanPreview = {
                controlsExpanded = false
                cleanPreview = true
            },
            onToggleRecording = {
                if (featureAvailability.processedRecordingAvailable) {
                    val activeSession = recordingSession
                    if (activeSession == null) {
                        recordingSession = ProcessedRecordingSession(
                            rootDirectory = recordingRootDirectory,
                            requestedOutputMode = recordingOutputMode,
                            actualOutputKind = RecordingOutputPolicy.outputKind(
                                requestedMode = recordingOutputMode,
                                usingGlPreview = usingGlPreview,
                            ),
                            videoRecorderFactory = { outputFile ->
                                if (
                                    RecordingOutputPolicy.outputKind(recordingOutputMode, usingGlPreview) ==
                                    RecordingOutputKind.CleanPreview
                                ) {
                                    GlProcessedMp4Recorder(outputFile)
                                } else {
                                    DebugProcessedMp4Recorder(outputFile)
                                }
                            },
                        )
                        lastRecordingPath = null
                    } else {
                        val output = activeSession.stop(
                            settings = analysisSettings,
                            thermalStatus = thermalStatus(context),
                            lightingDiagnostic = lightingDiagnostic,
                            rendererDiagnostics = RecordingRendererDiagnostics.from(
                                usingGlPreview = usingGlPreview,
                                glFrameStats = glFrameStats,
                            ),
                            visualizationModel = VisualizationModel.live(
                                settings = analysisSettings,
                                fullFrameColorPreview = liveEvmPreviewDecision.fullFrameColorPreview,
                                livePhasePreviewDecision = livePhasePreviewDecision,
                            ),
                        )
                        lastRecordingPath = output.absolutePath
                        recentRecordings = RecordingGallery.listRecent(recordingRootDirectory)
                        recordingSession = null
                    }
                }
            },
            onShareRecording = {
                lastRecordingPath?.let { shareRecordingMetadata(context, File(it)) }
            },
            onShareRecordingPath = { path ->
                shareRecordingMetadata(context, File(path))
            },
            onShareRecordingVideo = { path ->
                shareRecordingVideo(context, File(path))
            },
            onShareRecordingReport = { path ->
                shareRecordingReport(context, File(path))
            },
            onDeleteRecording = { item ->
                if (RecordingGallery.deleteItem(recordingRootDirectory, item)) {
                    recentRecordings = RecordingGallery.listRecent(recordingRootDirectory)
                    if (lastRecordingPath == item.metadataFile.absolutePath) {
                        lastRecordingPath = null
                    }
                }
            },
            onValidateVideo = {
                videoPickerLauncher.launch("video/*")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun CameraGlPreview(
    settings: AnalysisSettings,
    roiSource: RoiSource,
    manualRoi: NormalizedRect?,
    cameraControlsLocked: Boolean,
    liveEvmPreviewDecision: LiveEvmPreviewDecision,
    livePhasePreviewDecision: LivePhasePreviewDecision,
    onStats: (GlFrameStats) -> Unit,
    modifier: Modifier = Modifier,
    onSample: (AnalysisSample) -> CameraSampleResult,
    onProcessedFrame: (ProcessedGlFrame) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val colorParameters = remember { ColorMagnificationParameters() }
    val currentSettings = rememberUpdatedState(settings)
    val currentRoiSource = rememberUpdatedState(roiSource)
    val currentManualRoi = rememberUpdatedState(manualRoi)
    val currentOnSample = rememberUpdatedState(onSample)
    val currentLiveEvmPreviewDecision = rememberUpdatedState(liveEvmPreviewDecision)
    val currentLivePhasePreviewDecision = rememberUpdatedState(livePhasePreviewDecision)

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            lateinit var glView: GLSurfaceView
            lateinit var renderer: CameraOesRenderer
            glView = GLSurfaceView(viewContext).apply {
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                renderer = CameraOesRenderer(
                    requestRender = { glView.requestRender() },
                    onStats = onStats,
                    onProcessedFrame = onProcessedFrame,
                )
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                setZOrderOnTop(false)

                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener(
                    {
                        val cameraProvider = providerFuture.get()
                        val previewBuilder = Preview.Builder()
                        val analysisBuilder = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setResolutionSelector(
                                ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            android.util.Size(640, 480),
                                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                        ),
                                    )
                                    .build(),
                            )

                        applyPreviewCameraLocks(previewBuilder, cameraControlsLocked)
                        applyAnalysisCameraLocks(analysisBuilder, cameraControlsLocked)
                        applyPreviewTargetFps(previewBuilder)
                        applyAnalysisTargetFps(analysisBuilder)

                        val preview = previewBuilder.build().also {
                            it.setSurfaceProvider { request ->
                                renderer.setSurfaceRequest(request, mainExecutor)
                            }
                        }
                        val analysis = analysisBuilder.build().also {
                            it.setAnalyzer(
                                analysisExecutor,
                                PulseRoiAnalyzer(
                                    settingsProvider = { currentSettings.value },
                                    roiSourceProvider = { currentRoiSource.value },
                                    manualRoiProvider = { currentManualRoi.value },
                                ) { sample ->
                                    mainExecutor.execute {
                                        val activeSettings = currentSettings.value
                                        val sampleResult = currentOnSample.value(sample)
                                        renderer.setColorMagnificationUniforms(
                                            colorParameters.from(
                                                sample = sample,
                                                settings = activeSettings,
                                                fullFrameMode = currentLiveEvmPreviewDecision.value.fullFrameColorPreview,
                                                presentationTimestampNanos = sampleResult.presentationTimestampNanos,
                                                livePhasePreviewDecision = currentLivePhasePreviewDecision.value,
                                                lightingDiagnostic = sampleResult.lightingDiagnostic,
                                            )
                                        )
                                    }
                                },
                            )
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analysis,
                        )
                    },
                    mainExecutor,
                )
            }
            glView
        },
    )
}

private fun recordingsRoot(context: Context): File {
    val root = context.getExternalFilesDir(null) ?: context.filesDir
    return File(root, "recordings")
}

private fun thermalStatus(context: Context): String {
    val power = context.getSystemService(PowerManager::class.java)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        when (power.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "none"
            PowerManager.THERMAL_STATUS_LIGHT -> "light"
            PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
            PowerManager.THERMAL_STATUS_SEVERE -> "severe"
            PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
            else -> "unknown"
        }
    } else {
        "unavailable"
    }
}

private data class CameraSampleResult(
    val presentationTimestampNanos: Long,
    val lightingDiagnostic: LightingDiagnostic,
)

private fun shareRecordingMetadata(context: Context, metadataFile: File) {
    shareFile(context, metadataFile, mimeType = "application/json", title = "Share recording metadata")
}

private fun shareRecordingVideo(context: Context, videoFile: File) {
    shareFile(context, videoFile, mimeType = "video/mp4", title = "Share processed video")
}

private fun shareRecordingReport(context: Context, reportFile: File) {
    shareFile(context, reportFile, mimeType = "text/html", title = "Share evidence report")
}

private fun shareFile(
    context: Context,
    file: File,
    mimeType: String,
    title: String,
) {
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

@Composable
private fun AmplifiedTintOverlay(
    sample: AnalysisSample,
    settings: AnalysisSettings,
    artifactSuppressor: ArtifactSuppressor,
    mappingPolicy: PreviewRoiMappingPolicy,
    modifier: Modifier = Modifier,
) {
    val roi = sample.roi ?: return
    if (settings.viewMode == ViewMode.Raw) return

    val signal = artifactSuppressor.amplify(sample.bandpassedGreen, settings.amplification)
    val intensity = signal.normalizedMagnitude.toFloat()
    val tint = if (signal.value >= 0.0) {
        Color(0xFFFF6B6B).copy(alpha = overlayAlpha(settings.viewMode, intensity))
    } else {
        Color(0xFF3A86FF).copy(alpha = overlayAlpha(settings.viewMode, intensity))
    }

    Canvas(modifier = modifier) {
        val displayRoi = PreviewRoiMapper.mapAnalysisToPreview(
            roi = roi,
            frameSize = PreviewSize(sample.frameWidth, sample.frameHeight),
            previewSize = PreviewSize(size.width.toInt(), size.height.toInt()),
            rotationDegrees = sample.rotationDegrees,
            mirrorHorizontally = mappingPolicy.mirrorHorizontally,
        )
        drawRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(
                x = displayRoi.left * size.width,
                y = displayRoi.top * size.height,
            ),
            size = androidx.compose.ui.geometry.Size(
                width = displayRoi.width * size.width,
                height = displayRoi.height * size.height,
            ),
        )
    }
}

private fun overlayAlpha(viewMode: ViewMode, intensity: Float): Float {
    return when (viewMode) {
        ViewMode.Raw -> 0.0f
        ViewMode.Amplified -> 0.08f + intensity * 0.28f
        ViewMode.Difference -> 0.16f + intensity * 0.42f
        ViewMode.Split -> 0.08f + intensity * 0.28f
    }
}

@Composable
private fun RoiOverlay(
    sample: AnalysisSample,
    mappingPolicy: PreviewRoiMappingPolicy,
    modifier: Modifier = Modifier,
) {
    val roi = sample.roi ?: return
    Canvas(modifier = modifier) {
        val displayRoi = PreviewRoiMapper.mapAnalysisToPreview(
            roi = roi,
            frameSize = PreviewSize(sample.frameWidth, sample.frameHeight),
            previewSize = PreviewSize(size.width.toInt(), size.height.toInt()),
            rotationDegrees = sample.rotationDegrees,
            mirrorHorizontally = mappingPolicy.mirrorHorizontally,
        )
        drawRect(
            color = Color(0xFF00BFA5),
            topLeft = androidx.compose.ui.geometry.Offset(
                x = displayRoi.left * size.width,
                y = displayRoi.top * size.height,
            ),
            size = androidx.compose.ui.geometry.Size(
                width = displayRoi.width * size.width,
                height = displayRoi.height * size.height,
            ),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun ManualRoiOverlay(
    roi: NormalizedRect?,
    sample: AnalysisSample,
    mappingPolicy: PreviewRoiMappingPolicy,
    editing: Boolean,
    onRoiChanged: (NormalizedRect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    val inputModifier = if (editing) {
        modifier.pointerInput(sample.frameWidth, sample.frameHeight, sample.rotationDegrees) {
            detectDragGestures(
                onDragStart = { start ->
                    dragStart = start
                },
                onDrag = { change, _ ->
                    val start = dragStart ?: change.position
                    val previewRoi = ManualRoiSelector.fromDrag(
                        startX = start.x,
                        startY = start.y,
                        endX = change.position.x,
                        endY = change.position.y,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                    )
                    if (previewRoi != null) {
                        onRoiChanged(
                            PreviewRoiMapper.mapPreviewToAnalysis(
                                roi = previewRoi,
                                frameSize = PreviewSize(sample.frameWidth, sample.frameHeight),
                                previewSize = PreviewSize(size.width, size.height),
                                rotationDegrees = sample.rotationDegrees,
                                mirrorHorizontally = mappingPolicy.mirrorHorizontally,
                            )
                        )
                    }
                },
                onDragEnd = {
                    dragStart = null
                },
                onDragCancel = {
                    dragStart = null
                },
            )
        }
    } else {
        modifier
    }
    Canvas(
        modifier = inputModifier,
    ) {
        val selected = roi ?: return@Canvas
        val displayRoi = PreviewRoiMapper.mapAnalysisToPreview(
            roi = selected,
            frameSize = PreviewSize(sample.frameWidth, sample.frameHeight),
            previewSize = PreviewSize(size.width.toInt(), size.height.toInt()),
            rotationDegrees = sample.rotationDegrees,
            mirrorHorizontally = mappingPolicy.mirrorHorizontally,
        )
        drawRect(
            color = Color(0xFFFFC857),
            topLeft = androidx.compose.ui.geometry.Offset(
                x = displayRoi.left * size.width,
                y = displayRoi.top * size.height,
            ),
            size = androidx.compose.ui.geometry.Size(
                width = displayRoi.width * size.width,
                height = displayRoi.height * size.height,
            ),
            style = Stroke(width = 3.dp.toPx()),
        )
        if (editing) {
            val handleRadius = 6.dp.toPx()
            val left = displayRoi.left * size.width
            val top = displayRoi.top * size.height
            val right = displayRoi.right * size.width
            val bottom = displayRoi.bottom * size.height
            listOf(
                Offset(left, top),
                Offset(right, top),
                Offset(left, bottom),
                Offset(right, bottom),
            ).forEach { handle ->
                drawCircle(
                    color = Color(0xFFFFC857),
                    radius = handleRadius,
                    center = handle,
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    settings: AnalysisSettings,
    roiSource: RoiSource,
    manualRoi: NormalizedRect?,
    cameraControlsLocked: Boolean,
    modifier: Modifier = Modifier,
    onSample: (AnalysisSample) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val currentSettings = rememberUpdatedState(settings)
    val currentRoiSource = rememberUpdatedState(roiSource)
    val currentManualRoi = rememberUpdatedState(manualRoi)
    val currentOnSample = rememberUpdatedState(onSample)

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER

                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener(
                    {
                        val cameraProvider = providerFuture.get()
                        val previewBuilder = Preview.Builder()
                        val analysisBuilder = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setResolutionSelector(
                                ResolutionSelector.Builder()
                                    .setResolutionStrategy(
                                        ResolutionStrategy(
                                            android.util.Size(640, 480),
                                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                        ),
                                    )
                                    .build(),
                            )

                        applyPreviewCameraLocks(previewBuilder, cameraControlsLocked)
                        applyAnalysisCameraLocks(analysisBuilder, cameraControlsLocked)
                        applyPreviewTargetFps(previewBuilder)
                        applyAnalysisTargetFps(analysisBuilder)

                        val preview = previewBuilder.build().also {
                            it.surfaceProvider = surfaceProvider
                        }
                        val analysis = analysisBuilder.build()
                            .also {
                                it.setAnalyzer(
                                analysisExecutor,
                                PulseRoiAnalyzer(
                                    settingsProvider = { currentSettings.value },
                                    roiSourceProvider = { currentRoiSource.value },
                                    manualRoiProvider = { currentManualRoi.value },
                                ) { sample ->
                                    mainExecutor.execute { currentOnSample.value(sample) }
                                },
                                )
                            }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analysis,
                        )
                    },
                    mainExecutor,
                )
            }
        },
        update = {
            // Camera binding is intentionally created once in factory; frame analysis drives recomposition.
        },
    )
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun applyPreviewTargetFps(builder: Preview.Builder) {
    Camera2Interop.Extender(builder)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, TARGET_FPS_RANGE)
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun applyAnalysisTargetFps(builder: ImageAnalysis.Builder) {
    Camera2Interop.Extender(builder)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, TARGET_FPS_RANGE)
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun applyPreviewCameraLocks(
    builder: Preview.Builder,
    locked: Boolean,
) {
    if (!locked) return
    Camera2Interop.Extender(builder)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
}

private val TARGET_FPS_RANGE = Range(30, 30)

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun applyAnalysisCameraLocks(
    builder: ImageAnalysis.Builder,
    locked: Boolean,
) {
    if (!locked) return
    Camera2Interop.Extender(builder)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
}

@Composable
private fun PermissionPane(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101418))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Camera access powers the live preview and local magnification analysis.",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Frames stay on this device unless you choose to record or share metadata. This prototype is for visualization, not diagnosis.",
            color = Color(0xFFC8D3DC),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Continue")
        }
    }
}

@Composable
private fun UnavailablePane(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101418))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusOverlay(
    sample: AnalysisSample,
    signalHistory: List<Double>,
    breathingMotionSample: BreathingMotionSample,
    breathingMotionHistory: List<Double>,
    settings: AnalysisSettings,
    onSettingsChanged: (AnalysisSettings) -> Unit,
    cameraControlsLocked: Boolean,
    onCameraControlsLockedChanged: (Boolean) -> Unit,
    qualityCuesEnabled: Boolean,
    onQualityCuesEnabledChanged: (Boolean) -> Unit,
    roiSource: RoiSource,
    onRoiSourceChanged: (RoiSource) -> Unit,
    manualRoi: NormalizedRect?,
    manualRoiEditing: Boolean,
    onManualRoiEditingChanged: (Boolean) -> Unit,
    onClearManualRoi: () -> Unit,
    onResetSettings: () -> Unit,
    showGlDebug: Boolean,
    onShowGlDebugChanged: (Boolean) -> Unit,
    usingGlPreview: Boolean,
    featureAvailability: FeatureAvailability,
    glFrameStats: GlFrameStats,
    liveEvmPreviewDecision: LiveEvmPreviewDecision,
    livePhasePreviewDecision: LivePhasePreviewDecision,
    isRecording: Boolean,
    recordingElapsedMillis: Long,
    lastRecordingPath: String?,
    recentRecordings: List<RecordingGalleryItem>,
    validationSummary: String?,
    validationRunning: Boolean,
    lightingDiagnostic: LightingDiagnostic?,
    qualityStatuses: List<QualityStatus>,
    recordingOutputMode: RecordingOutputMode,
    onRecordingOutputModeChanged: (RecordingOutputMode) -> Unit,
    controlsExpanded: Boolean,
    initialExpandedPanel: ExpandedPanelTab,
    cleanPreview: Boolean,
    onShowControls: () -> Unit,
    onHideControls: () -> Unit,
    onEnterCleanPreview: () -> Unit,
    onToggleRecording: () -> Unit,
    onShareRecording: () -> Unit,
    onShareRecordingPath: (String) -> Unit,
    onShareRecordingVideo: (String) -> Unit,
    onShareRecordingReport: (String) -> Unit,
    onDeleteRecording: (RecordingGalleryItem) -> Unit,
    onValidateVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPanel by remember {
        mutableStateOf(initialExpandedPanel)
    }

    if (!controlsExpanded && cleanPreview) {
        CleanPreviewOverlay(
            isRecording = isRecording,
            recordingElapsedMillis = recordingElapsedMillis,
            onShowControls = onShowControls,
            modifier = modifier,
        )
        return
    }

    if (!controlsExpanded) {
        CompactStatusOverlay(
            sample = sample,
            settings = settings,
            onSettingsChanged = onSettingsChanged,
            signalHistory = signalHistory,
            breathingMotionHistory = breathingMotionHistory,
            qualityStatuses = qualityStatuses,
            isRecording = isRecording,
            recordingElapsedMillis = recordingElapsedMillis,
            roiSource = roiSource,
            manualRoi = manualRoi,
            onShowControls = onShowControls,
            onEnterCleanPreview = onEnterCleanPreview,
            modifier = modifier,
        )
        return
    }

    val liveVisualizationModel = VisualizationModel.live(
        settings = settings,
        fullFrameColorPreview = liveEvmPreviewDecision.fullFrameColorPreview,
        livePhasePreviewDecision = livePhasePreviewDecision,
    )

    Column(
        modifier = modifier
            .background(Color(0x99000000))
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mode: ${settings.mode.label}", color = Color.White)
            Button(onClick = onHideControls) {
                Text("Hide")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Output: ${settings.mode.outputLabel}",
            color = Color(0xFFC8D3DC),
        )
        Text(
            text = "Preview: ${liveEvmPreviewDecision.label}",
            color = Color(0xFFC8D3DC),
        )
        Text(
            text = "Signal: ${liveVisualizationModel.signalSource.label}",
            color = Color(0xFFC8D3DC),
        )
        Text(
            text = "Renderer: ${liveVisualizationModel.renderer.label}",
            color = Color(0xFFC8D3DC),
        )
        Spacer(modifier = Modifier.height(4.dp))
        QualityStatusRow(qualityStatuses)
        Spacer(modifier = Modifier.height(8.dp))
        ExpandedPanelTabs(
            selectedPanel = selectedPanel,
            onSelectedPanelChanged = { selectedPanel = it },
        )
        Spacer(modifier = Modifier.height(8.dp))
        when (selectedPanel) {
            ExpandedPanelTab.Controls -> {
                ModeControls(
                    settings = settings,
                    onSettingsChanged = onSettingsChanged,
                    availableModes = featureAvailability.availableModes,
                    cameraControlsLocked = cameraControlsLocked,
                    onCameraControlsLockedChanged = onCameraControlsLockedChanged,
                    qualityCuesEnabled = qualityCuesEnabled,
                    onQualityCuesEnabledChanged = onQualityCuesEnabledChanged,
                    roiSource = roiSource,
                    onRoiSourceChanged = onRoiSourceChanged,
                    manualRoi = manualRoi,
                    manualRoiEditing = manualRoiEditing,
                    onManualRoiEditingChanged = onManualRoiEditingChanged,
                    onClearManualRoi = onClearManualRoi,
                    onResetSettings = onResetSettings,
                    showGlDebug = showGlDebug,
                    onShowGlDebugChanged = onShowGlDebugChanged,
                    usingGlPreview = usingGlPreview,
                    glPreviewAvailable = featureAvailability.glPreviewAvailable,
                )
            }
            ExpandedPanelTab.Setup -> {
                DemoPresetControls(
                    settings = settings,
                    onSettingsChanged = onSettingsChanged,
                )
                Spacer(modifier = Modifier.height(8.dp))
                SetupGuidePanel(settings.mode)
                ParityPresetPanel(
                    mode = settings.mode,
                    analysisFps = sample.analysisFps,
                )
            }
            ExpandedPanelTab.Recording -> {
                RecordingControls(
                    isRecording = isRecording,
                    elapsedMillis = recordingElapsedMillis,
                    lastRecordingPath = lastRecordingPath,
                    recentRecordings = recentRecordings,
                    recordingOutputMode = recordingOutputMode,
                    onRecordingOutputModeChanged = onRecordingOutputModeChanged,
                    onToggleRecording = onToggleRecording,
                    onShareRecording = onShareRecording,
                    onShareRecordingPath = onShareRecordingPath,
                    onShareRecordingVideo = onShareRecordingVideo,
                    onShareRecordingReport = onShareRecordingReport,
                    onDeleteRecording = onDeleteRecording,
                    recordingAvailable = featureAvailability.processedRecordingAvailable,
                    validationAvailable = featureAvailability.recordedVideoValidationAvailable,
                    validationSummary = validationSummary,
                    validationRunning = validationRunning,
                    onValidateVideo = onValidateVideo,
                )
            }
            ExpandedPanelTab.Debug -> {
                DebugPanel(
                    sample = sample,
                    signalHistory = signalHistory,
                    breathingMotionSample = breathingMotionSample,
                    breathingMotionHistory = breathingMotionHistory,
                    settings = settings,
                    showGlDebug = showGlDebug,
                    glFrameStats = glFrameStats,
                    lightingDiagnostic = lightingDiagnostic,
                )
            }
        }
    }
}

@Composable
private fun ExpandedPanelTabs(
    selectedPanel: ExpandedPanelTab,
    onSelectedPanelChanged: (ExpandedPanelTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpandedPanelTab.entries.forEach { panel ->
            CompactControlButton(
                label = panel.label,
                onClick = { onSelectedPanelChanged(panel) },
                enabled = selectedPanel != panel,
                modifier = Modifier.weight(1.0f),
            )
        }
    }
}

@Composable
private fun DebugPanel(
    sample: AnalysisSample,
    signalHistory: List<Double>,
    breathingMotionSample: BreathingMotionSample,
    breathingMotionHistory: List<Double>,
    settings: AnalysisSettings,
    showGlDebug: Boolean,
    glFrameStats: GlFrameStats,
    lightingDiagnostic: LightingDiagnostic?,
) {
    Text(
        "Analysis: ${"%.1f".format(sample.analysisFps)} fps / ${"%.0f".format(sample.latencyMillis)} ms",
        color = Color.White,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Green: ${"%.1f".format(sample.averageGreen)}", color = Color.White)
        Text(
            "Band: ${"%+.3f".format(sample.bandpassedGreen)} / ${if (sample.timestampMonotonic) "Timing OK" else "Timing jump"}",
            color = Color.White,
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Translation: dx ${"%+.3f".format(sample.translation.dx)} dy ${"%+.3f".format(sample.translation.dy)}",
        color = Color.White,
    )
    lightingDiagnostic?.let { diagnostic ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Lighting: ${diagnostic.label} - ${diagnostic.action}",
            color = Color.White,
        )
    }
    if (showGlDebug) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "GL camera: ${"%.1f".format(glFrameStats.averageFps)} fps / render ${"%.2f".format(glFrameStats.averageFrameMillis)} ms",
            color = Color.White,
        )
        Text(
            text = "GL renderer: ${glFrameStats.renderPath.label}",
            color = Color.White,
        )
        Text(
            text = glFrameStats.reconstructionDiagnostics.summary(),
            color = Color.White,
        )
        Text(
            text = glFrameStats.phaseDiagnostics.summary,
            color = Color.White,
        )
        Text(
            text = PerformanceBenchmark.from(sample, glFrameStats).summary(),
            color = Color.White,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    SignalWaveform(
        values = signalHistory,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    )
    if (settings.mode == MagnificationMode.Breathing) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Breathing motion: ${"%+.4f".format(breathingMotionSample.amplifiedDy)}",
            color = Color.White,
        )
        SignalWaveform(
            values = breathingMotionHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
        )
    }
}

@Composable
private fun CompactStatusOverlay(
    sample: AnalysisSample,
    settings: AnalysisSettings,
    onSettingsChanged: (AnalysisSettings) -> Unit,
    signalHistory: List<Double>,
    breathingMotionHistory: List<Double>,
    qualityStatuses: List<QualityStatus>,
    isRecording: Boolean,
    recordingElapsedMillis: Long,
    roiSource: RoiSource,
    manualRoi: NormalizedRect?,
    onShowControls: () -> Unit,
    onEnterCleanPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color(0x73000000))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onShowControls) {
                Text("Controls")
            }
            Button(onClick = onEnterCleanPreview) {
                Text("Clean")
            }
            Text(
                text = settings.mode.compactOutputLabel,
                color = Color.White,
                modifier = Modifier.weight(1.0f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = RoiSourcePolicy.labelFor(roiSource, sample.roiState),
                color = roiStateColor(sample.roiState),
                maxLines = 1,
            )
            Text(
                text = if (isRecording) "REC ${formatElapsed(recordingElapsedMillis)}" else qualityStatuses.joinToString { it.label },
                color = if (isRecording) Color(0xFFFF6B6B) else qualityColor(qualityStatuses),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${"%.0f".format(sample.analysisFps)} fps",
                color = Color.White,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ViewMode.entries.forEach { viewMode ->
                CompactControlButton(
                    label = viewMode.compactLabel,
                    onClick = { onSettingsChanged(settings.copy(viewMode = viewMode)) },
                    enabled = settings.viewMode != viewMode,
                    modifier = Modifier.weight(1.0f),
                )
            }
        }
        val compactHistory = SignalDisplayPolicy.compactSignalHistory(
            mode = settings.mode,
            pulseHistory = signalHistory,
            breathingHistory = breathingMotionHistory,
        )
        if (compactHistory.size >= 2) {
            Spacer(modifier = Modifier.height(6.dp))
            SignalWaveform(
                values = compactHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
            )
        }
    }
}

@Composable
private fun CleanPreviewOverlay(
    isRecording: Boolean,
    recordingElapsedMillis: Long,
    onShowControls: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onShowControls) {
            Text("Controls")
        }
        if (isRecording) {
            Text(
                text = "REC ${formatElapsed(recordingElapsedMillis)}",
                color = Color(0xFFFF6B6B),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun QualityStatusRow(statuses: List<QualityStatus>) {
    val hasWarning = statuses != listOf(QualityStatus.Good)
    Column {
        Text(
            text = "Quality: ${statuses.joinToString { it.label }}",
            color = qualityColor(statuses),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (hasWarning) {
                statuses.joinToString("  ") { "${it.label}: ${it.action}" }
            } else {
                ""
            },
            modifier = Modifier.height(20.dp),
            color = Color(0xFFC8D3DC),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun qualityColor(statuses: List<QualityStatus>): Color {
    return if (statuses == listOf(QualityStatus.Good)) Color(0xFF00BFA5) else Color(0xFFFFC857)
}

private fun roiStateColor(state: RoiState): Color {
    return when (state) {
        RoiState.Manual -> Color(0xFFFFC857)
        RoiState.FullFrame -> Color(0xFFC8D3DC)
        RoiState.Tracking -> Color(0xFF00BFA5)
        RoiState.Frozen -> Color(0xFFFFC857)
        RoiState.Center -> Color(0xFFC8D3DC)
    }
}

@Composable
private fun DemoPresetControls(
    settings: AnalysisSettings,
    onSettingsChanged: (AnalysisSettings) -> Unit,
) {
    Text(
        text = "Demo presets",
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DemoPreset.entries.forEach { preset ->
            CompactControlButton(
                label = preset.label,
                onClick = { onSettingsChanged(preset.settings) },
                enabled = settings != preset.settings,
                modifier = Modifier.weight(1.0f),
            )
        }
    }
}

@Composable
private fun SetupGuidePanel(mode: MagnificationMode) {
    val guide = SetupGuide.forMode(mode)
    Column {
        Text(
            text = guide.title,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = guide.target,
            color = Color(0xFFC8D3DC),
            style = MaterialTheme.typography.bodySmall,
        )
        guide.stabilize.forEachIndexed { index, step ->
            Text(
                text = "${index + 1}. $step",
                color = Color(0xFFC8D3DC),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = guide.expected,
            color = Color(0xFFC8D3DC),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ParityPresetPanel(
    mode: MagnificationMode,
    analysisFps: Double,
) {
    val preset = ParityPreset.forMode(mode)
    val warnings = ParityPresetWarnings.forPreset(preset, analysisFps)
    Column {
        Text(
            text = "Locked preset: ${preset.label} ${preset.bandLabel}",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Light: ${preset.lighting}; support: ${preset.support}; distance: ${preset.distance}.",
            color = Color(0xFFC8D3DC),
            style = MaterialTheme.typography.bodySmall,
        )
        warnings.forEach { warning ->
            Text(
                text = warning,
                color = Color(0xFFFFC857),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    elapsedMillis: Long,
    lastRecordingPath: String?,
    recentRecordings: List<RecordingGalleryItem>,
    recordingOutputMode: RecordingOutputMode,
    onRecordingOutputModeChanged: (RecordingOutputMode) -> Unit,
    onToggleRecording: () -> Unit,
    onShareRecording: () -> Unit,
    onShareRecordingPath: (String) -> Unit,
    onShareRecordingVideo: (String) -> Unit,
    onShareRecordingReport: (String) -> Unit,
    onDeleteRecording: (RecordingGalleryItem) -> Unit,
    recordingAvailable: Boolean,
    validationAvailable: Boolean,
    validationSummary: String?,
    validationRunning: Boolean,
    onValidateVideo: () -> Unit,
) {
    if (recordingAvailable) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onToggleRecording) {
                Text(if (isRecording) "Stop Recording" else "Start Recording")
            }
            Text(
                text = if (isRecording) {
                    "REC ${formatElapsed(elapsedMillis)}"
                } else {
                    "Recorder idle"
                },
                color = if (isRecording) Color(0xFFFF6B6B) else Color.White,
            )
        }
        if (!isRecording) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RecordingOutputMode.entries.forEach { mode ->
                    CompactControlButton(
                        label = mode.label,
                        onClick = { onRecordingOutputModeChanged(mode) },
                        enabled = recordingOutputMode != mode,
                        modifier = Modifier.weight(1.0f),
                    )
                }
            }
            Text(
                text = recordingOutputMode.description,
                color = Color(0xFFC8D3DC),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (validationAvailable) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onValidateVideo,
            enabled = !validationRunning,
        ) {
            Text(if (validationRunning) "Processing Video" else "Process Video")
        }
    }
    if (!validationSummary.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = validationSummary,
            color = Color.White,
        )
    }
    if (!lastRecordingPath.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onShareRecording) {
            Text("Share Metadata")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Metadata saved: $lastRecordingPath",
            color = Color.White,
        )
    }
    if (recentRecordings.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Recent recordings",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
        )
        recentRecordings.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.detail,
                    color = Color.White,
                    modifier = Modifier.weight(1.0f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(onClick = { onShareRecordingPath(item.metadataFile.absolutePath) }) {
                    Text("Metadata")
                }
                item.debugVideoPath?.let { videoPath ->
                    Button(onClick = { onShareRecordingVideo(videoPath) }) {
                        Text("Video")
                    }
                }
                item.evidenceReportPath?.let { reportPath ->
                    Button(onClick = { onShareRecordingReport(reportPath) }) {
                        Text("Report")
                    }
                }
                Button(onClick = { onDeleteRecording(item) }) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatElapsed(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun copyUriToCacheFile(context: Context, uri: Uri, displayName: String?): File {
    val outputFile = File(context.cacheDir, sanitizeVideoFileName(displayName ?: "selected-video.mp4"))
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Could not open selected video" }
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return outputFile
}

private data class ProcessedVideoOutcome(
    val summary: String,
    val metadataFile: File?,
)

private fun processRecordedVideoExport(
    inputFile: File,
    sourceName: String,
    settings: AnalysisSettings,
    rootDirectory: File,
): ProcessedVideoOutcome {
    val startedAtMillis = System.currentTimeMillis()
    val decodeOptions = RecordedVideoDecodeOptions(
        maxFrames = (TenSecondValidationFlow.TARGET_DURATION_MILLIS / 1000L * 30L).toInt(),
    )
    val frames = RecordedVideoFrameDecoder().decode(inputFile, decodeOptions)
    val report = RecordedVideoAnalysisRunner(settings).analyze(frames)
    val validation = RecordedVideoValidationResult(
        sourceName = sourceName,
        settings = settings,
        report = report,
    )
    val processing = RecordedVideoProcessor(settings).process(frames)
    val sessionDirectory = File(rootDirectory, processedVideoSessionName(startedAtMillis)).apply { mkdirs() }
    val outputVideo = File(sessionDirectory, "debug_processed.mp4")
    if (processing.hasFrames) {
        RecordedVideoMp4Exporter().export(processing.processedFrames, outputVideo)
    }
    val timelineFile = File(sessionDirectory, "signal_timeline.csv")
    if (processing.hasFrames) {
        timelineFile.writeText(RecordedVideoEvidenceTimeline.toCsv(processing.processedFrames))
    }
    val qualitySummary = recordedVideoQualitySummary(
        frameCount = report.frameCount,
        bandpassedEnergy = report.bandpassedEnergy,
        timestampsMonotonic = report.timestampsMonotonic,
    )
    val evidenceReportFile = File(sessionDirectory, "evidence_report.html")
    if (processing.hasFrames) {
        evidenceReportFile.writeText(
            RecordedVideoEvidenceReport.toHtml(
                sourceName = sourceName,
                settings = settings,
                frames = processing.processedFrames,
                qualitySummary = qualitySummary,
            )
        )
    }
    val metadataFile = File(sessionDirectory, "metadata.json")
    metadataFile.writeText(
        recordedVideoExportMetadata(
            sourceName = sourceName,
            startedAtMillis = startedAtMillis,
            durationMillis = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L),
            settings = settings,
            frameCount = report.frameCount,
            averageFps = report.averageFps,
            bandpassedEnergy = report.bandpassedEnergy,
            maxBandpassedMagnitude = report.maxBandpassedMagnitude,
            timestampsMonotonic = report.timestampsMonotonic,
            rateEstimate = report.rateEstimate,
            debugVideoPath = outputVideo.takeIf { it.exists() }?.absolutePath,
            timelinePath = timelineFile.takeIf { it.exists() }?.absolutePath,
            evidenceReportPath = evidenceReportFile.takeIf { it.exists() }?.absolutePath,
            processedFrameCount = processing.processedFrames.size,
            qualitySummary = qualitySummary,
        )
    )
    val exportText = if (outputVideo.exists()) {
        ", export saved: ${outputVideo.name}"
    } else {
        ", no export frames"
    }
    return ProcessedVideoOutcome(
        summary = validation.summary() + exportText,
        metadataFile = metadataFile,
    )
}

private fun processedVideoSessionName(startedAtMillis: Long): String {
    return "processed-${DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(startedAtMillis))}"
        .replace(":", "-")
        .replace(".", "-")
        .lowercase(Locale.US)
}

private fun recordedVideoExportMetadata(
    sourceName: String,
    startedAtMillis: Long,
    durationMillis: Long,
    settings: AnalysisSettings,
    frameCount: Int,
    averageFps: Double,
    bandpassedEnergy: Double,
    maxBandpassedMagnitude: Double,
    timestampsMonotonic: Boolean,
    rateEstimate: com.dnrohr.eulerianmagnification.analysis.GatedRateEstimate,
    debugVideoPath: String?,
    timelinePath: String?,
    evidenceReportPath: String?,
    processedFrameCount: Int,
    qualitySummary: String,
): String {
    val visualizationModel = VisualizationModel.recorded(settings)
    val validationFields = TenSecondValidationFlow.metadataFields(TenSecondValidationFlow.setup(settings))
    return buildString {
        appendLine("{")
        appendLine("  \"sourceName\": ${sourceName.quoteJson()},")
        appendLine("  \"validationFlow\": ${validationFields.getValue("validationFlow").quoteJson()},")
        appendLine("  \"targetDurationMillis\": ${validationFields.getValue("targetDurationMillis")},")
        appendLine("  \"requiredArtifacts\": ${validationFields.getValue("requiredArtifacts").quoteJson()},")
        appendLine("  \"startedAtMillis\": $startedAtMillis,")
        appendLine("  \"durationMillis\": $durationMillis,")
        appendLine("  \"mode\": ${settings.mode.label.quoteJson()},")
        appendLine("  \"viewMode\": ${settings.viewMode.label.quoteJson()},")
        appendLine("  \"signalSource\": ${visualizationModel.signalSource.id.quoteJson()},")
        appendLine("  \"signalSourceLabel\": ${visualizationModel.signalSource.label.quoteJson()},")
        appendLine("  \"renderer\": ${visualizationModel.renderer.id.quoteJson()},")
        appendLine("  \"rendererLabel\": ${visualizationModel.renderer.label.quoteJson()},")
        appendLine("  \"visualizationStyle\": ${visualizationModel.visualizationStyle.id.quoteJson()},")
        appendLine("  \"visualizationStyleLabel\": ${visualizationModel.visualizationStyle.label.quoteJson()},")
        appendLine("  \"amplification\": ${settings.amplification},")
        appendLine("  \"lowCutHz\": ${settings.lowCutHz},")
        appendLine("  \"highCutHz\": ${settings.highCutHz},")
        appendLine("  \"debugVideoPath\": ${debugVideoPath?.quoteJson() ?: "null"},")
        appendLine("  \"timelinePath\": ${timelinePath?.quoteJson() ?: "null"},")
        appendLine("  \"evidenceReportPath\": ${evidenceReportPath?.quoteJson() ?: "null"},")
        appendLine("  \"sampleCount\": $frameCount,")
        appendLine("  \"frameCount\": $frameCount,")
        appendLine("  \"processedFrameCount\": $processedFrameCount,")
        appendLine("  \"averageFps\": ${averageFps.formatJsonNumber()},")
        appendLine("  \"bandpassedEnergy\": ${bandpassedEnergy.formatJsonNumber()},")
        appendLine("  \"maxBandpassedMagnitude\": ${maxBandpassedMagnitude.formatJsonNumber()},")
        appendLine("  \"timestampsMonotonic\": $timestampsMonotonic,")
        appendLine("  \"experimentalRateVisible\": ${rateEstimate.visible},")
        appendLine("  \"experimentalRatePerMinute\": ${rateEstimate.estimate?.perMinute?.formatJsonNumber() ?: "null"},")
        appendLine("  \"experimentalRateUnit\": ${rateEstimate.estimate?.kind?.unitLabel?.quoteJson() ?: "null"},")
        appendLine("  \"experimentalRateNonDiagnostic\": ${rateEstimate.estimate?.diagnostic?.not() ?: true},")
        appendLine("  \"experimentalRateHiddenReason\": ${rateEstimate.hiddenReason?.message?.quoteJson() ?: "null"},")
        appendLine("  \"qualitySummary\": ${qualitySummary.quoteJson()}")
        appendLine("}")
    }
}

private fun recordedVideoQualitySummary(
    frameCount: Int,
    bandpassedEnergy: Double,
    timestampsMonotonic: Boolean,
): String {
    return when {
        frameCount == 0 -> "no frames"
        !timestampsMonotonic -> "timing issue"
        bandpassedEnergy <= 0.0 -> "weak signal"
        else -> "signal present"
    }
}

private fun String.quoteJson(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

private fun Number.formatJsonNumber(): String {
    return String.format(Locale.US, "%.6f", toDouble())
}

private fun displayNameForUri(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) cursor.getString(nameIndex) else null
        } else {
            null
        }
    }
}

private fun sanitizeVideoFileName(name: String): String {
    val cleaned = name
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .trim('_')
    return cleaned.ifBlank { "selected-video.mp4" }
}

@Composable
private fun ModeControls(
    settings: AnalysisSettings,
    onSettingsChanged: (AnalysisSettings) -> Unit,
    availableModes: List<MagnificationMode>,
    cameraControlsLocked: Boolean,
    onCameraControlsLockedChanged: (Boolean) -> Unit,
    qualityCuesEnabled: Boolean,
    onQualityCuesEnabledChanged: (Boolean) -> Unit,
    roiSource: RoiSource,
    onRoiSourceChanged: (RoiSource) -> Unit,
    manualRoi: NormalizedRect?,
    manualRoiEditing: Boolean,
    onManualRoiEditingChanged: (Boolean) -> Unit,
    onClearManualRoi: () -> Unit,
    onResetSettings: () -> Unit,
    showGlDebug: Boolean,
    onShowGlDebugChanged: (Boolean) -> Unit,
    usingGlPreview: Boolean,
    glPreviewAvailable: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        availableModes.forEach { mode ->
            CompactControlButton(
                label = mode.compactLabel,
                onClick = { onSettingsChanged(settings.copy(mode = mode)) },
                enabled = settings.mode != mode,
                modifier = Modifier.weight(1.0f),
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ViewMode.entries.forEach { viewMode ->
            CompactControlButton(
                label = viewMode.compactLabel,
                onClick = { onSettingsChanged(settings.copy(viewMode = viewMode)) },
                enabled = settings.viewMode != viewMode,
                modifier = Modifier.weight(1.0f),
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Amplification ${"%.1f".format(settings.amplification)}x",
        color = Color.White,
    )
    Slider(
        value = settings.amplification,
        onValueChange = { onSettingsChanged(settings.copy(amplification = it)) },
        valueRange = 1.0f..30.0f,
    )
    Text(
        text = "Band ${"%.1f".format(settings.lowCutHz)}-${"%.1f".format(settings.highCutHz)} Hz",
        color = Color.White,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = { onCameraControlsLockedChanged(!cameraControlsLocked) }) {
        Text(if (cameraControlsLocked) "Unlock AE/AWB" else "Lock AE/AWB")
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = { onQualityCuesEnabledChanged(!qualityCuesEnabled) }) {
        Text(if (qualityCuesEnabled) "Quality Cues On" else "Quality Cues Off")
    }
    Text(
        text = "Optional haptic cue for major quality changes, rate-limited.",
        color = Color(0xFFC8D3DC),
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "ROI Source",
        color = Color.White,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoiSource.entries.forEach { source ->
            CompactControlButton(
                label = source.compactLabel,
                onClick = { onRoiSourceChanged(source) },
                enabled = roiSource != source,
                modifier = Modifier.weight(1.0f),
            )
        }
    }
    Text(
        text = RoiSourcePolicy.descriptionFor(roiSource),
        color = Color(0xFFC8D3DC),
        style = MaterialTheme.typography.bodySmall,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = {
        onRoiSourceChanged(RoiSource.Manual)
        onManualRoiEditingChanged(if (manualRoiEditing) ManualRoiEditState.afterDoneEditing() else true)
    }) {
        Text(if (manualRoiEditing) "Done ROI" else "Edit ROI")
    }
    if (manualRoiEditing) {
        Text(
            text = "Drag on the preview to place one manual ROI.",
            color = Color(0xFFC8D3DC),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (manualRoi != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClearManualRoi) {
            Text("Clear ROI")
        }
    }
    if (glPreviewAvailable) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onShowGlDebugChanged(!showGlDebug) },
            enabled = settings.viewMode != ViewMode.Split,
        ) {
            Text(if (usingGlPreview) "Use CameraX Preview" else "Use GL Preview")
        }
        if (settings.viewMode == ViewMode.Split) {
            Text(
                text = "Split uses GL preview for live raw-vs-processed comparison.",
                color = Color(0xFFC8D3DC),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = "CameraX is the standard camera preview and CPU analysis path. GL is the GPU preview/processing path for live Split and phase motion; it should become the main motion path, with CameraX kept as a stable fallback.",
            color = Color(0xFFC8D3DC),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onResetSettings) {
        Text("Reset Settings")
    }
}

@Composable
private fun CompactControlButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = 0.dp, minHeight = 48.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
        )
    }
}

private val MagnificationMode.compactLabel: String
    get() = when (this) {
        MagnificationMode.Pulse -> "Pulse"
        MagnificationMode.Breathing -> "Breath"
        MagnificationMode.Tremor -> "Motion"
        MagnificationMode.ObjectVibration -> "Object"
    }

private val ViewMode.compactLabel: String
    get() = when (this) {
        ViewMode.Raw -> "Raw"
        ViewMode.Amplified -> "Amp"
        ViewMode.Difference -> "Diff"
        ViewMode.Split -> "Split"
    }

@Composable
private fun SignalWaveform(
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxMagnitude = values.maxOf { abs(it) }.coerceAtLeast(0.001)
        val centerY = size.height / 2.0f
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)

        for (index in 1 until values.size) {
            val previous = values[index - 1]
            val current = values[index]
            drawLine(
                color = Color(0xFFFFC857),
                start = androidx.compose.ui.geometry.Offset(
                    x = (index - 1) * stepX,
                    y = centerY - (previous / maxMagnitude).toFloat() * centerY,
                ),
                end = androidx.compose.ui.geometry.Offset(
                    x = index * stepX,
                    y = centerY - (current / maxMagnitude).toFloat() * centerY,
                ),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

private const val SIGNAL_HISTORY_SIZE = 120
private const val UI_TELEMETRY_INTERVAL_MILLIS = 250L
