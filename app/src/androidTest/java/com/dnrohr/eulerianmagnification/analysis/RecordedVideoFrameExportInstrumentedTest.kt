package com.dnrohr.eulerianmagnification.analysis

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class RecordedVideoFrameExportInstrumentedTest {
    @Test
    fun exportsDecodedLuminanceFramesForRieszValidation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        val sampleFile = arguments.getString("sampleAssetName")?.let { assetName ->
            instrumentation.context.assets.open(assetName).use { input ->
                File(instrumentation.targetContext.cacheDir, assetName).also { output ->
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { input.copyTo(it) }
                }
            }
        } ?: File(
            requireNotNull(arguments.getString("sampleVideoPath")) {
                "sampleAssetName or sampleVideoPath instrumentation argument is required"
            }
        )
        val outputFile = arguments.getString("outputJsonPath")?.let(::File)
            ?: File(
                File(instrumentation.targetContext.filesDir, "validation"),
                "${sampleFile.nameWithoutExtension}-riesz-frames.json",
            )
        assertTrue("sample file must exist: ${sampleFile.absolutePath}", sampleFile.exists())

        val frames = RecordedVideoFrameDecoder().decode(
            sampleFile,
            RecordedVideoDecodeOptions(targetFps = 10.0, maxFrames = 60),
        )
        assertTrue("sample should decode at least 20 frames", frames.size >= 20)

        outputFile.parentFile?.mkdirs()
        outputFile.writeText(
            buildString {
                appendLine("{")
                appendLine("  \"source\": \"${sampleFile.name}\",")
                appendLine("  \"targetFps\": 10.0,")
                appendLine("  \"frameCount\": ${frames.size},")
                appendLine("  \"width\": $EXPORT_WIDTH,")
                appendLine("  \"height\": $EXPORT_HEIGHT,")
                appendLine("  \"frames\": [")
                frames.forEachIndexed { index, frame ->
                    append(downsampledLuminanceJson(frame, indent = "    "))
                    if (index != frames.lastIndex) append(",")
                    appendLine()
                }
                appendLine("  ]")
                appendLine("}")
            }
        )
        assertTrue("export file should be non-empty", outputFile.length() > 0L)
    }

    private fun downsampledLuminanceJson(
        frame: RgbFrame,
        indent: String,
    ): String {
        return buildString {
            appendLine("$indent[")
            for (y in 0 until EXPORT_HEIGHT) {
                append("$indent  [")
                for (x in 0 until EXPORT_WIDTH) {
                    append("%.6f".format(Locale.US, frame.averageLuminanceForBin(x, y)))
                    if (x != EXPORT_WIDTH - 1) append(", ")
                }
                append("]")
                if (y != EXPORT_HEIGHT - 1) append(",")
                appendLine()
            }
            append("$indent]")
        }
    }

    private fun RgbFrame.averageLuminanceForBin(
        binX: Int,
        binY: Int,
    ): Double {
        val left = binX * width / EXPORT_WIDTH
        val right = ((binX + 1) * width / EXPORT_WIDTH).coerceAtLeast(left + 1)
        val top = binY * height / EXPORT_HEIGHT
        val bottom = ((binY + 1) * height / EXPORT_HEIGHT).coerceAtLeast(top + 1)
        var total = 0.0
        var count = 0
        for (y in top until bottom.coerceAtMost(height)) {
            for (x in left until right.coerceAtMost(width)) {
                val color = pixels[y * width + x]
                val red = (color shr 16) and 0xff
                val green = (color shr 8) and 0xff
                val blue = color and 0xff
                total += (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0
                count++
            }
        }
        return if (count == 0) 0.0 else total / count.toDouble()
    }

    companion object {
        private const val EXPORT_WIDTH = 24
        private const val EXPORT_HEIGHT = 16
    }
}
