package com.dnrohr.eulerianmagnification.analysis

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ParityHarnessInstrumentedTest {
    @Test
    fun writesDecodedVideoParityArtifacts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        val arguments = InstrumentationRegistry.getArguments()
        val sampleFile = arguments.getString("sampleAssetName")?.let { assetName ->
            instrumentation.context.assets.open(assetName).use { input ->
                File(targetContext.cacheDir, assetName).also { output ->
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { input.copyTo(it) }
                }
            }
        } ?: arguments.getString("sampleVideoPath")?.let(::File)
            ?: instrumentation.context.assets.open(DEFAULT_ASSET).use { input ->
                File(targetContext.cacheDir, DEFAULT_ASSET).also { output ->
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { input.copyTo(it) }
                }
            }
        val sampleId = arguments.getString("sampleId")
            ?: sampleFile.nameWithoutExtension
        val outputRoot = arguments.getString("outputDirPath")?.let(::File)
            ?: File(targetContext.getExternalFilesDir(null), "parity-output")

        assertTrue("sample file must exist: ${sampleFile.absolutePath}", sampleFile.exists())
        val frames = RecordedVideoFrameDecoder().decode(
            sampleFile,
            RecordedVideoDecodeOptions(
                targetFps = TARGET_FPS,
                maxFrames = MAX_FRAMES,
                maxFrameWidth = MAX_FRAME_WIDTH,
            ),
        ).map { it.fitWithin(MAX_FRAME_WIDTH) }
        assertTrue("sample should decode at least 20 frames", frames.size >= 20)

        val sampleSpec = SampleVideoCatalog.byId(sampleId)
        val sample = ParityHarnessSample(
            id = sampleSpec?.id ?: sampleId,
            displayName = sampleSpec?.displayName ?: sampleFile.nameWithoutExtension,
            targetClass = sampleSpec?.expectedUse ?: "Decoded video parity sample",
            frames = frames,
            settings = AnalysisSettings(
                mode = sampleSpec?.recommendedMode ?: MagnificationMode.Pulse,
                amplification = DEFAULT_AMPLIFICATION,
            ),
            sourcePath = sampleFile.absolutePath,
            sourceSha256 = sampleFile.sha256(),
        )

        val bundle = ParityHarnessArtifactWriter(outputRoot).write(
            ParityHarness().run(sample, viewModes = ViewMode.entries),
        )

        assertTrue("manifest should exist", bundle.manifestFile.isFile)
        assertTrue("report should exist", bundle.htmlReportFile.isFile)
        assertTrue("artifact index should exist", bundle.indexFile.isFile)
        assertTrue(bundle.manifestFile.readText().contains("\"frameCount\": ${frames.size}"))
        assertTrue(bundle.viewArtifacts.all { it.previewFrameFiles.isNotEmpty() })
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02X".format(byte) }
    }

    private fun RgbFrame.fitWithin(maxWidth: Int): RgbFrame {
        if (width <= maxWidth) return this
        val targetWidth = maxWidth
        val targetHeight = (height * (targetWidth / width.toDouble())).toInt().coerceAtLeast(1)
        val outputPixels = IntArray(targetWidth * targetHeight)
        for (y in 0 until targetHeight) {
            val sourceY = (y * height / targetHeight).coerceIn(0, height - 1)
            for (x in 0 until targetWidth) {
                val sourceX = (x * width / targetWidth).coerceIn(0, width - 1)
                outputPixels[y * targetWidth + x] = pixels[sourceY * width + sourceX]
            }
        }
        return RgbFrame(
            width = targetWidth,
            height = targetHeight,
            timestampNanos = timestampNanos,
            pixels = outputPixels,
        )
    }

    private companion object {
        private const val DEFAULT_ASSET = "mit-evm-baby.mp4"
        private const val TARGET_FPS = 6.0
        private const val MAX_FRAMES = 36
        private const val MAX_FRAME_WIDTH = 160
        private const val DEFAULT_AMPLIFICATION = 8.0f
    }
}
