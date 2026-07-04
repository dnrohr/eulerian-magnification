package com.dnrohr.eulerianmagnification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.opengl.GLSurfaceView
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.analysis.MagnificationMode
import com.dnrohr.eulerianmagnification.analysis.PulseRoiAnalyzer
import com.dnrohr.eulerianmagnification.analysis.ViewMode
import com.dnrohr.eulerianmagnification.capabilities.CapabilityReportStore
import com.dnrohr.eulerianmagnification.capabilities.CapabilityReporter
import com.dnrohr.eulerianmagnification.gl.CameraOesRenderer
import com.dnrohr.eulerianmagnification.gl.ColorMagnificationParameters
import com.dnrohr.eulerianmagnification.gl.GlFrameStats
import com.dnrohr.eulerianmagnification.quality.ArtifactSuppressor
import com.dnrohr.eulerianmagnification.quality.LightingFlickerDetector
import com.dnrohr.eulerianmagnification.quality.QualityEvaluator
import com.dnrohr.eulerianmagnification.quality.QualityStatus
import com.dnrohr.eulerianmagnification.recording.DebugProcessedMp4Recorder
import com.dnrohr.eulerianmagnification.recording.ProcessedRecordingSession
import com.dnrohr.eulerianmagnification.ui.AppTheme
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val capabilityReporter = CapabilityReporter(this)
        capabilityReporter.logSummary()
        CapabilityReportStore(this).writeLatestReport(capabilityReporter)

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
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
    var analysisSample by remember { mutableStateOf(AnalysisSample()) }
    var analysisSettings by remember { mutableStateOf(AnalysisSettings()) }
    var recordingSession by remember { mutableStateOf<ProcessedRecordingSession?>(null) }
    var lastRecordingPath by remember { mutableStateOf<String?>(null) }
    var cameraControlsLocked by remember { mutableStateOf(false) }
    var showGlDebug by remember { mutableStateOf(false) }
    var glFrameStats by remember { mutableStateOf(GlFrameStats()) }
    val qualityEvaluator = remember { QualityEvaluator() }
    val lightingFlickerDetector = remember { LightingFlickerDetector() }
    val artifactSuppressor = remember { ArtifactSuppressor() }
    var lightingFlickerLikely by remember { mutableStateOf(false) }
    val signalHistory = remember { mutableStateListOf<Double>() }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            if (showGlDebug) {
                key(analysisSettings, cameraControlsLocked) {
                    CameraGlPreview(
                        settings = analysisSettings,
                        cameraControlsLocked = cameraControlsLocked,
                        onStats = { glFrameStats = it },
                        modifier = Modifier.fillMaxSize(),
                        onSample = {
                            analysisSample = it
                            lightingFlickerLikely = lightingFlickerDetector.update(it.averageGreen)
                            recordingSession?.record(it, analysisSettings)
                            signalHistory.add(it.bandpassedGreen)
                            if (signalHistory.size > SIGNAL_HISTORY_SIZE) {
                                signalHistory.removeAt(0)
                            }
                        },
                    )
                }
            } else key(analysisSettings, cameraControlsLocked) {
                CameraPreview(
                    settings = analysisSettings,
                    cameraControlsLocked = cameraControlsLocked,
                    modifier = Modifier.fillMaxSize(),
                    onSample = {
                        analysisSample = it
                        lightingFlickerLikely = lightingFlickerDetector.update(it.averageGreen)
                        recordingSession?.record(it, analysisSettings)
                        signalHistory.add(it.bandpassedGreen)
                        if (signalHistory.size > SIGNAL_HISTORY_SIZE) {
                            signalHistory.removeAt(0)
                        }
                    },
                )
            }
            AmplifiedTintOverlay(
                sample = analysisSample,
                settings = analysisSettings,
                artifactSuppressor = artifactSuppressor,
                modifier = Modifier.fillMaxSize(),
            )
            RoiOverlay(
                sample = analysisSample,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PermissionPane(onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            })
        }

        StatusOverlay(
            sample = analysisSample,
            signalHistory = signalHistory,
            settings = analysisSettings,
            onSettingsChanged = { analysisSettings = it },
            cameraControlsLocked = cameraControlsLocked,
            onCameraControlsLockedChanged = { cameraControlsLocked = it },
            showGlDebug = showGlDebug,
            onShowGlDebugChanged = { showGlDebug = it },
            glFrameStats = glFrameStats,
            isRecording = recordingSession != null,
            recordingElapsedMillis = recordingSession?.elapsedMillis ?: 0L,
            lastRecordingPath = lastRecordingPath,
            qualityStatuses = qualityEvaluator.evaluate(
                sample = analysisSample,
                lightingFlickerLikely = lightingFlickerLikely,
            ),
            onToggleRecording = {
                val activeSession = recordingSession
                if (activeSession == null) {
                    recordingSession = ProcessedRecordingSession(
                        rootDirectory = recordingsRoot(context),
                        videoRecorderFactory = { outputFile -> DebugProcessedMp4Recorder(outputFile) },
                    )
                    lastRecordingPath = null
                } else {
                    val output = activeSession.stop(
                        settings = analysisSettings,
                        thermalStatus = thermalStatus(context),
                    )
                    lastRecordingPath = output.absolutePath
                    recordingSession = null
                }
            },
            onShareRecording = {
                lastRecordingPath?.let { shareRecordingMetadata(context, File(it)) }
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
    cameraControlsLocked: Boolean,
    onStats: (GlFrameStats) -> Unit,
    modifier: Modifier = Modifier,
    onSample: (AnalysisSample) -> Unit,
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
                                PulseRoiAnalyzer(settings) { sample ->
                                    renderer.setColorMagnificationUniforms(colorParameters.from(sample, settings))
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
    }
}

@Composable
private fun RoiOverlay(
    sample: AnalysisSample,
    modifier: Modifier = Modifier,
) {
    val roi = sample.roi ?: return
    Canvas(modifier = modifier) {
        drawRect(
            color = Color(0xFF00BFA5),
            topLeft = androidx.compose.ui.geometry.Offset(
                x = roi.left * size.width,
                y = roi.top * size.height,
            ),
            size = androidx.compose.ui.geometry.Size(
                width = (roi.right - roi.left) * size.width,
                height = (roi.bottom - roi.top) * size.height,
            ),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun CameraPreview(
    settings: AnalysisSettings,
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
                                    PulseRoiAnalyzer(settings) { sample ->
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
            text = "Camera access is needed to preview and magnify live video.",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Allow Camera")
        }
    }
}

@Composable
private fun StatusOverlay(
    sample: AnalysisSample,
    signalHistory: List<Double>,
    settings: AnalysisSettings,
    onSettingsChanged: (AnalysisSettings) -> Unit,
    cameraControlsLocked: Boolean,
    onCameraControlsLockedChanged: (Boolean) -> Unit,
    showGlDebug: Boolean,
    onShowGlDebugChanged: (Boolean) -> Unit,
    glFrameStats: GlFrameStats,
    isRecording: Boolean,
    recordingElapsedMillis: Long,
    lastRecordingPath: String?,
    qualityStatuses: List<QualityStatus>,
    onToggleRecording: () -> Unit,
    onShareRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        }
        Spacer(modifier = Modifier.height(4.dp))
        QualityStatusRow(qualityStatuses)
        Spacer(modifier = Modifier.height(8.dp))
        ModeControls(
            settings = settings,
            onSettingsChanged = onSettingsChanged,
            cameraControlsLocked = cameraControlsLocked,
            onCameraControlsLockedChanged = onCameraControlsLockedChanged,
            showGlDebug = showGlDebug,
            onShowGlDebugChanged = onShowGlDebugChanged,
        )
        Spacer(modifier = Modifier.height(8.dp))
        RecordingControls(
            isRecording = isRecording,
            elapsedMillis = recordingElapsedMillis,
            lastRecordingPath = lastRecordingPath,
            onToggleRecording = onToggleRecording,
            onShareRecording = onShareRecording,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SignalWaveform(
            values = signalHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        )
    }
}

@Composable
private fun QualityStatusRow(statuses: List<QualityStatus>) {
    val isGood = statuses == listOf(QualityStatus.Good)
    Text(
        text = "Quality: ${statuses.joinToString { it.label }}",
        color = if (isGood) Color(0xFF00BFA5) else Color(0xFFFFC857),
    )
}

@Composable
private fun RecordingControls(
    isRecording: Boolean,
    elapsedMillis: Long,
    lastRecordingPath: String?,
    onToggleRecording: () -> Unit,
    onShareRecording: () -> Unit,
) {
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
}

private fun formatElapsed(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun ModeControls(
    settings: AnalysisSettings,
    onSettingsChanged: (AnalysisSettings) -> Unit,
    cameraControlsLocked: Boolean,
    onCameraControlsLockedChanged: (Boolean) -> Unit,
    showGlDebug: Boolean,
    onShowGlDebugChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MagnificationMode.entries.forEach { mode ->
            Button(
                onClick = { onSettingsChanged(settings.copy(mode = mode)) },
                enabled = settings.mode != mode,
                modifier = Modifier.weight(1.0f),
            ) {
                Text(mode.label)
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ViewMode.entries.forEach { viewMode ->
            Button(
                onClick = { onSettingsChanged(settings.copy(viewMode = viewMode)) },
                enabled = settings.viewMode != viewMode,
                modifier = Modifier.weight(1.0f),
            ) {
                Text(viewMode.label)
            }
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
    Button(onClick = { onShowGlDebugChanged(!showGlDebug) }) {
        Text(if (showGlDebug) "Use CameraX Preview" else "Use GL Preview")
    }
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
