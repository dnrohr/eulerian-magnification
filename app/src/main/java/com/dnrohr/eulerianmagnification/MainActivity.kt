package com.dnrohr.eulerianmagnification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.opengl.GLSurfaceView
import android.provider.OpenableColumns
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.dnrohr.eulerianmagnification.analysis.PreviewSize
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoDecodeOptions
import com.dnrohr.eulerianmagnification.analysis.RecordedVideoValidator
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
import com.dnrohr.eulerianmagnification.quality.LightingFlickerDetector
import com.dnrohr.eulerianmagnification.quality.QualityEvaluator
import com.dnrohr.eulerianmagnification.quality.QualityStatus
import com.dnrohr.eulerianmagnification.recording.DebugProcessedMp4Recorder
import com.dnrohr.eulerianmagnification.recording.GlProcessedMp4Recorder
import com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSession
import com.dnrohr.eulerianmagnification.recording.RecordingGallery
import com.dnrohr.eulerianmagnification.recording.RecordingGalleryItem
import com.dnrohr.eulerianmagnification.ui.AppTheme
import java.io.File
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
                    MainScreen(featureAvailability)
                }
            }
        }
    }
}

@Composable
private fun MainScreen(featureAvailability: FeatureAvailability) {
    val context = LocalContext.current
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
    val validationExecutor = remember { Executors.newSingleThreadExecutor() }
    var analysisSample by remember { mutableStateOf(AnalysisSample()) }
    var analysisSettings by remember {
        mutableStateOf(AnalysisSettings(mode = featureAvailability.availableModes.firstOrNull() ?: MagnificationMode.Pulse))
    }
    var recordingSession by remember { mutableStateOf<ProcessedRecordingSession?>(null) }
    var lastRecordingPath by remember { mutableStateOf<String?>(null) }
    var recentRecordings by remember { mutableStateOf(RecordingGallery.listRecent(recordingRootDirectory)) }
    var validationSummary by remember { mutableStateOf<String?>(null) }
    var validationRunning by remember { mutableStateOf(false) }
    var cameraControlsLocked by remember { mutableStateOf(false) }
    var showGlDebug by remember { mutableStateOf(false) }
    var controlsExpanded by remember { mutableStateOf(false) }
    var manualRoi by remember { mutableStateOf<NormalizedRect?>(null) }
    var glFrameStats by remember { mutableStateOf(GlFrameStats()) }
    val qualityEvaluator = remember { QualityEvaluator() }
    val lightingFlickerDetector = remember { LightingFlickerDetector() }
    val artifactSuppressor = remember { ArtifactSuppressor() }
    val breathingMotionFilter = remember(analysisSettings.mode, analysisSettings.amplification) {
        BreathingMotionFilter(amplification = analysisSettings.amplification)
    }
    var lightingFlickerLikely by remember { mutableStateOf(false) }
    var breathingMotionSample by remember { mutableStateOf(BreathingMotionSample()) }
    val signalHistory = remember { mutableStateListOf<Double>() }
    val breathingMotionHistory = remember { mutableStateListOf<Double>() }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            validationRunning = true
            validationSummary = "Video processing: running"
            val settings = analysisSettings
            validationExecutor.execute {
                val summary = runCatching {
                    val inputFile = copyUriToCacheFile(context, uri, displayNameForUri(context, uri))
                    RecordedVideoValidator().validate(
                        file = inputFile,
                        settings = settings,
                        decodeOptions = RecordedVideoDecodeOptions(maxFrames = 300),
                    ).summary()
                }.getOrElse { error ->
                    "Video processing failed: ${error.message ?: error::class.java.simpleName}"
                }
                ContextCompat.getMainExecutor(context).execute {
                    validationSummary = summary
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
        if (analysisSettings.mode != MagnificationMode.Breathing) {
            breathingMotionHistory.clear()
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

    fun handleSample(sample: AnalysisSample): Long {
        analysisSample = sample
        lightingFlickerLikely = lightingFlickerDetector.update(sample.averageGreen)
        val presentationTimestampNanos = recordingSession
            ?.record(sample, analysisSettings)
            ?.presentationTimestampNanos
            ?: sample.frameTimestampNanos.coerceAtLeast(0L)
        signalHistory.add(sample.bandpassedGreen)
        if (signalHistory.size > SIGNAL_HISTORY_SIZE) {
            signalHistory.removeAt(0)
        }
        if (analysisSettings.mode == MagnificationMode.Breathing) {
            val motion = breathingMotionFilter.update(
                translation = sample.translation,
                timestampNanos = sample.frameTimestampNanos,
            )
            breathingMotionSample = motion
            breathingMotionHistory.add(motion.amplifiedDy)
            if (breathingMotionHistory.size > SIGNAL_HISTORY_SIZE) {
                breathingMotionHistory.removeAt(0)
            }
        }
        return presentationTimestampNanos
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission && featureAvailability.liveCameraAvailable) {
            if (showGlDebug) {
                key(analysisSettings, manualRoi, cameraControlsLocked) {
                    CameraGlPreview(
                        settings = analysisSettings,
                        manualRoi = manualRoi,
                        cameraControlsLocked = cameraControlsLocked,
                        onStats = { glFrameStats = it },
                        modifier = Modifier.fillMaxSize(),
                        onSample = ::handleSample,
                        onProcessedFrame = { frame -> recordingSession?.record(frame) },
                    )
                }
            } else key(analysisSettings, manualRoi, cameraControlsLocked) {
                CameraPreview(
                    settings = analysisSettings,
                    manualRoi = manualRoi,
                    cameraControlsLocked = cameraControlsLocked,
                    modifier = Modifier.fillMaxSize(),
                    onSample = { sample -> handleSample(sample) },
                )
            }
            AmplifiedTintOverlay(
                sample = analysisSample,
                settings = analysisSettings,
                artifactSuppressor = artifactSuppressor,
                modifier = Modifier.fillMaxSize(),
            )
            if (manualRoi == null) {
                RoiOverlay(
                    sample = analysisSample,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            ManualRoiOverlay(
                roi = manualRoi,
                sample = analysisSample,
                onRoiChanged = { manualRoi = it },
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
            sample = analysisSample,
            signalHistory = signalHistory,
            breathingMotionSample = breathingMotionSample,
            breathingMotionHistory = breathingMotionHistory,
            settings = analysisSettings,
            onSettingsChanged = { analysisSettings = it },
            cameraControlsLocked = cameraControlsLocked,
            onCameraControlsLockedChanged = { cameraControlsLocked = it },
            manualRoi = manualRoi,
            onClearManualRoi = { manualRoi = null },
            showGlDebug = showGlDebug,
            onShowGlDebugChanged = { showGlDebug = it },
            featureAvailability = featureAvailability,
            glFrameStats = glFrameStats,
            isRecording = recordingSession != null,
            recordingElapsedMillis = recordingSession?.elapsedMillis ?: 0L,
            lastRecordingPath = lastRecordingPath,
            recentRecordings = recentRecordings,
            validationSummary = validationSummary,
            validationRunning = validationRunning,
            qualityStatuses = qualityEvaluator.evaluate(
                sample = analysisSample,
                settings = analysisSettings,
                lightingFlickerLikely = lightingFlickerLikely,
            ),
            controlsExpanded = controlsExpanded,
            onToggleControlsExpanded = { controlsExpanded = !controlsExpanded },
            onToggleRecording = {
                if (featureAvailability.processedRecordingAvailable) {
                    val activeSession = recordingSession
                    if (activeSession == null) {
                        recordingSession = ProcessedRecordingSession(
                            rootDirectory = recordingRootDirectory,
                            videoRecorderFactory = { outputFile ->
                                if (showGlDebug) {
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
    manualRoi: NormalizedRect?,
    cameraControlsLocked: Boolean,
    onStats: (GlFrameStats) -> Unit,
    modifier: Modifier = Modifier,
    onSample: (AnalysisSample) -> Long,
    onProcessedFrame: (ProcessedGlFrame) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val colorParameters = remember { ColorMagnificationParameters() }

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

                        val preview = previewBuilder.build().also {
                            it.setSurfaceProvider { request ->
                                renderer.setSurfaceRequest(request, mainExecutor)
                            }
                        }
                        val analysis = analysisBuilder.build().also {
                            it.setAnalyzer(
                                analysisExecutor,
                                PulseRoiAnalyzer(settings, manualRoi = manualRoi) { sample ->
                                    mainExecutor.execute {
                                        val presentationTimestampNanos = onSample(sample)
                                        renderer.setColorMagnificationUniforms(
                                            colorParameters.from(
                                                sample = sample,
                                                settings = settings,
                                                presentationTimestampNanos = presentationTimestampNanos,
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

private fun shareRecordingMetadata(context: Context, metadataFile: File) {
    if (!metadataFile.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        metadataFile,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share recording metadata"))
}

@Composable
private fun AmplifiedTintOverlay(
    sample: AnalysisSample,
    settings: AnalysisSettings,
    artifactSuppressor: ArtifactSuppressor,
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
        drawRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(
                x = roi.left * size.width,
                y = roi.top * size.height,
            ),
            size = androidx.compose.ui.geometry.Size(
                width = roi.width * size.width,
                height = roi.height * size.height,
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
    modifier: Modifier = Modifier,
) {
    val roi = sample.roi ?: return
    Canvas(modifier = modifier) {
        val displayRoi = PreviewRoiMapper.mapAnalysisToPreview(
            roi = roi,
            frameSize = PreviewSize(sample.frameWidth, sample.frameHeight),
            previewSize = PreviewSize(size.width.toInt(), size.height.toInt()),
            rotationDegrees = sample.rotationDegrees,
            mirrorHorizontally = true,
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
    onRoiChanged: (NormalizedRect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    Canvas(
        modifier = modifier.pointerInput(sample.frameWidth, sample.frameHeight, sample.rotationDegrees) {
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
                                mirrorHorizontally = true,
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
        },
    ) {
        val selected = roi ?: return@Canvas
        val displayRoi = PreviewRoiMapper.mapAnalysisToPreview(
            roi = selected,
            frameSize = PreviewSize(sample.frameWidth, sample.frameHeight),
            previewSize = PreviewSize(size.width.toInt(), size.height.toInt()),
            rotationDegrees = sample.rotationDegrees,
            mirrorHorizontally = true,
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
    }
}

@Composable
private fun CameraPreview(
    settings: AnalysisSettings,
    manualRoi: NormalizedRect?,
    cameraControlsLocked: Boolean,
    modifier: Modifier = Modifier,
    onSample: (AnalysisSample) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

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

                        val preview = previewBuilder.build().also {
                            it.surfaceProvider = surfaceProvider
                        }
                        val analysis = analysisBuilder.build()
                            .also {
                                it.setAnalyzer(
                                analysisExecutor,
                                PulseRoiAnalyzer(settings, manualRoi = manualRoi) { sample ->
                                    mainExecutor.execute { onSample(sample) }
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
private fun applyPreviewCameraLocks(
    builder: Preview.Builder,
    locked: Boolean,
) {
    if (!locked) return
    Camera2Interop.Extender(builder)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
}

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
    manualRoi: NormalizedRect?,
    onClearManualRoi: () -> Unit,
    showGlDebug: Boolean,
    onShowGlDebugChanged: (Boolean) -> Unit,
    featureAvailability: FeatureAvailability,
    glFrameStats: GlFrameStats,
    isRecording: Boolean,
    recordingElapsedMillis: Long,
    lastRecordingPath: String?,
    recentRecordings: List<RecordingGalleryItem>,
    validationSummary: String?,
    validationRunning: Boolean,
    qualityStatuses: List<QualityStatus>,
    controlsExpanded: Boolean,
    onToggleControlsExpanded: () -> Unit,
    onToggleRecording: () -> Unit,
    onShareRecording: () -> Unit,
    onShareRecordingPath: (String) -> Unit,
    onValidateVideo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!controlsExpanded) {
        CompactStatusOverlay(
            sample = sample,
            settings = settings,
            qualityStatuses = qualityStatuses,
            isRecording = isRecording,
            recordingElapsedMillis = recordingElapsedMillis,
            manualRoi = manualRoi,
            onToggleControlsExpanded = onToggleControlsExpanded,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .background(Color(0x99000000))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mode: ${settings.mode.label}", color = Color.White)
            Button(onClick = onToggleControlsExpanded) {
                Text("Hide")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Output: ${settings.mode.outputLabel}",
            color = Color(0xFFC8D3DC),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Analysis: ${"%.1f".format(sample.analysisFps)} fps / ${"%.0f".format(sample.latencyMillis)} ms",
                color = Color.White,
            )
        }
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
        if (showGlDebug) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "GL: ${"%.1f".format(glFrameStats.averageFps)} fps / ${"%.2f".format(glFrameStats.averageFrameMillis)} ms",
                color = Color.White,
            )
            Text(
                text = PerformanceBenchmark.from(sample, glFrameStats).summary(),
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        QualityStatusRow(qualityStatuses)
        Spacer(modifier = Modifier.height(8.dp))
        ModeControls(
            settings = settings,
            onSettingsChanged = onSettingsChanged,
            availableModes = featureAvailability.availableModes,
            cameraControlsLocked = cameraControlsLocked,
            onCameraControlsLockedChanged = onCameraControlsLockedChanged,
            manualRoi = manualRoi,
            onClearManualRoi = onClearManualRoi,
            showGlDebug = showGlDebug,
            onShowGlDebugChanged = onShowGlDebugChanged,
            glPreviewAvailable = featureAvailability.glPreviewAvailable,
        )
        Spacer(modifier = Modifier.height(8.dp))
        RecordingControls(
            isRecording = isRecording,
            elapsedMillis = recordingElapsedMillis,
            lastRecordingPath = lastRecordingPath,
            recentRecordings = recentRecordings,
            onToggleRecording = onToggleRecording,
            onShareRecording = onShareRecording,
            onShareRecordingPath = onShareRecordingPath,
            recordingAvailable = featureAvailability.processedRecordingAvailable,
            validationAvailable = featureAvailability.recordedVideoValidationAvailable,
            validationSummary = validationSummary,
            validationRunning = validationRunning,
            onValidateVideo = onValidateVideo,
        )
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
}

@Composable
private fun CompactStatusOverlay(
    sample: AnalysisSample,
    settings: AnalysisSettings,
    qualityStatuses: List<QualityStatus>,
    isRecording: Boolean,
    recordingElapsedMillis: Long,
    manualRoi: NormalizedRect?,
    onToggleControlsExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color(0x73000000))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onToggleControlsExpanded) {
            Text("Controls")
        }
        Text(
            text = settings.mode.compactOutputLabel,
            color = Color.White,
            modifier = Modifier.weight(1.0f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (manualRoi == null) "Auto ROI" else "Manual ROI",
            color = if (manualRoi == null) Color(0xFF00BFA5) else Color(0xFFFFC857),
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
}

@Composable
private fun QualityStatusRow(statuses: List<QualityStatus>) {
    Column {
        Text(
            text = "Quality: ${statuses.joinToString { it.label }}",
            color = qualityColor(statuses),
        )
        if (statuses != listOf(QualityStatus.Good)) {
            Text(
                text = statuses.joinToString("  ") { "${it.label}: ${it.action}" },
                color = Color(0xFFC8D3DC),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun qualityColor(statuses: List<QualityStatus>): Color {
    return if (statuses == listOf(QualityStatus.Good)) Color(0xFF00BFA5) else Color(0xFFFFC857)
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    elapsedMillis: Long,
    lastRecordingPath: String?,
    recentRecordings: List<RecordingGalleryItem>,
    onToggleRecording: () -> Unit,
    onShareRecording: () -> Unit,
    onShareRecordingPath: (String) -> Unit,
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
                    text = item.summary,
                    color = Color.White,
                    modifier = Modifier.weight(1.0f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(onClick = { onShareRecordingPath(item.metadataFile.absolutePath) }) {
                    Text("Share")
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
    manualRoi: NormalizedRect?,
    onClearManualRoi: () -> Unit,
    showGlDebug: Boolean,
    onShowGlDebugChanged: (Boolean) -> Unit,
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
    if (manualRoi != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClearManualRoi) {
            Text("Clear ROI")
        }
    }
    if (glPreviewAvailable) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onShowGlDebugChanged(!showGlDebug) }) {
            Text(if (showGlDebug) "Use CameraX Preview" else "Use GL Preview")
        }
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
