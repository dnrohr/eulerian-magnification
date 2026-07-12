package com.dnrohr.eulerianmagnification.recording

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import androidx.test.platform.app.InstrumentationRegistry
import com.dnrohr.eulerianmagnification.analysis.AnalysisSample
import com.dnrohr.eulerianmagnification.analysis.AnalysisSettings
import com.dnrohr.eulerianmagnification.gl.GlTextureSize
import com.dnrohr.eulerianmagnification.gl.ProcessedGlFrame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class ProcessedRecordingSessionInstrumentedTest {
    @Test
    fun recordsShortCleanGlSessionWithValidMp4AndMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "processed-session-smoke").apply {
            deleteRecursively()
            mkdirs()
        }
        val session = ProcessedRecordingSession(
            rootDirectory = root,
            startedAtMillis = 1_700_000_000_000L,
            requestedOutputMode = RecordingOutputMode.Clean,
            actualOutputKind = RecordingOutputKind.CleanPreview,
            videoRecorderFactory = { outputFile ->
                GlProcessedMp4Recorder(outputFile, bitrate = 600_000, frameRate = 30)
            },
        )

        OffscreenGlContext(GlTextureSize(64, 64)).use {
            val textureId = createTexture(64, 64)
            repeat(5) { index ->
                updateTexture(textureId, 64, 64, frameIndex = index)
                val timestampNanos = index * FRAME_INTERVAL_NANOS
                session.record(AnalysisSample(frameTimestampNanos = timestampNanos))
                session.record(
                    ProcessedGlFrame(
                        textureId = textureId,
                        size = GlTextureSize(64, 64),
                        presentationTimestampNanos = timestampNanos,
                        splitMode = false,
                    )
                )
            }
            GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
        }

        val metadataFile = session.stop(
            settings = AnalysisSettings(),
            thermalStatus = "none",
        )
        val videoFile = File(metadataFile.parentFile, "debug_processed.mp4")
        val validation = EncodedOutputValidator().validate(videoFile)
        val metadata = metadataFile.readText()

        assertTrue(validation.errors.joinToString(), validation.isValid)
        assertTrue("encoded video should not be tiny", videoFile.length() > MIN_EXPECTED_MP4_BYTES)
        assertTrue(metadata.contains("\"recordingOutputKind\": \"clean_preview\""))
        assertTrue(metadata.contains("\"debugVideoPath\": ${videoFile.absolutePath.quoteJson()}"))
        assertTrue(metadata.contains("\"sampleCount\": 5"))
    }

    private fun createTexture(width: Int, height: Int): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        updateTexture(textures[0], width, height, frameIndex = 0)
        return textures[0]
    }

    private fun updateTexture(
        textureId: Int,
        width: Int,
        height: Int,
        frameIndex: Int,
    ) {
        val pixels = ByteArray(width * height * 4)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = (y * width + x) * 4
                pixels[offset] = ((x + frameIndex * 9) and 0xFF).toByte()
                pixels[offset + 1] = ((y + frameIndex * 13) and 0xFF).toByte()
                pixels[offset + 2] = (96 + frameIndex * 20).coerceAtMost(255).toByte()
                pixels[offset + 3] = 255.toByte()
            }
        }
        val buffer = ByteBuffer.allocateDirect(pixels.size).put(pixels).apply { position(0) }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            width,
            height,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            buffer,
        )
    }

    private fun String.quoteJson(): String {
        return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }

    private class OffscreenGlContext(
        private val size: GlTextureSize,
    ) : AutoCloseable {
        private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var context: EGLContext = EGL14.EGL_NO_CONTEXT
        private var surface: EGLSurface = EGL14.EGL_NO_SURFACE

        init {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "Could not get EGL display." }
            val version = IntArray(2)
            check(EGL14.eglInitialize(display, version, 0, version, 1)) { "Could not initialize EGL." }
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            check(
                EGL14.eglChooseConfig(
                    display,
                    intArrayOf(
                        EGL14.EGL_RED_SIZE, 8,
                        EGL14.EGL_GREEN_SIZE, 8,
                        EGL14.EGL_BLUE_SIZE, 8,
                        EGL14.EGL_ALPHA_SIZE, 8,
                        EGL14.EGL_RENDERABLE_TYPE, 0x00000040,
                        EGL14.EGL_NONE,
                    ),
                    0,
                    configs,
                    0,
                    1,
                    configCount,
                    0,
                ) && configCount[0] > 0
            ) { "Could not choose EGL config." }
            context = EGL14.eglCreateContext(
                display,
                configs[0],
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
                0,
            )
            check(context != EGL14.EGL_NO_CONTEXT) { "Could not create EGL context." }
            surface = EGL14.eglCreatePbufferSurface(
                display,
                configs[0],
                intArrayOf(
                    EGL14.EGL_WIDTH, size.width,
                    EGL14.EGL_HEIGHT, size.height,
                    EGL14.EGL_NONE,
                ),
                0,
            )
            check(surface != EGL14.EGL_NO_SURFACE) { "Could not create EGL pbuffer surface." }
            check(EGL14.eglMakeCurrent(display, surface, surface, context)) {
                "Could not make EGL context current."
            }
        }

        override fun close() {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                EGL14.eglReleaseThread()
                EGL14.eglTerminate(display)
            }
        }
    }

    companion object {
        private const val FRAME_INTERVAL_NANOS = 33_333_333L
        private const val MIN_EXPECTED_MP4_BYTES = 512L
    }
}
