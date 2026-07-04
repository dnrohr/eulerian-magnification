package com.dnrohr.eulerianmagnification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.PulseRoiAnalyzer
import com.dnrohr.eulerianmagnification.capabilities.CapabilityReporter
import com.dnrohr.eulerianmagnification.ui.AppTheme
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CapabilityReporter(this).logSummary()

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
    val signalHistory = remember { mutableStateListOf<Double>() }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onSample = {
                    analysisSample = it
                    signalHistory.add(it.bandpassedGreen)
                    if (signalHistory.size > SIGNAL_HISTORY_SIZE) {
                        signalHistory.removeAt(0)
                    }
                },
            )
            AmplifiedTintOverlay(
                sample = analysisSample,
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun AmplifiedTintOverlay(
    sample: AnalysisSample,
    modifier: Modifier = Modifier,
) {
    val roi = sample.roi ?: return
    val intensity = (abs(sample.bandpassedGreen) / 6.0).coerceIn(0.0, 1.0).toFloat()
    val tint = if (sample.bandpassedGreen >= 0.0) {
        Color(0xFFFF6B6B).copy(alpha = 0.08f + intensity * 0.20f)
    } else {
        Color(0xFF3A86FF).copy(alpha = 0.08f + intensity * 0.16f)
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
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
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
                            .build()
                            .also {
                                it.setAnalyzer(
                                    analysisExecutor,
                                    PulseRoiAnalyzer { sample ->
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
            Text("Mode: Pulse ROI probe", color = Color.White)
            Text("Analysis: ${"%.1f".format(sample.analysisFps)} fps", color = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Green: ${"%.1f".format(sample.averageGreen)}", color = Color.White)
            Text("Band: ${"%+.3f".format(sample.bandpassedGreen)}", color = Color.White)
        }
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
