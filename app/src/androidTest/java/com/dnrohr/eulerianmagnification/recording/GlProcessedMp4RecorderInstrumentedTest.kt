package com.dnrohr.eulerianmagnification.recording

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import androidx.test.platform.app.InstrumentationRegistry
import com.dnrohr.eulerianmagnification.gl.GlTextureSize
import com.dnrohr.eulerianmagnification.gl.ProcessedGlFrame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GlProcessedMp4RecorderInstrumentedTest {
    @Test
    fun encodesProcessedGlFramesToValidMp4() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputFile = File(context.cacheDir, "gl-processed-instrumented.mp4")
        outputFile.delete()

        OffscreenGlContext(GlTextureSize(64, 64)).use {
            val textureId = createTexture(64, 64)
            val recorder = GlProcessedMp4Recorder(outputFile, bitrate = 600_000, frameRate = 30)
            repeat(5) { index ->
                updateTexture(textureId, 64, 64, index)
                recorder.record(
                    ProcessedGlFrame(
                        textureId = textureId,
                        size = GlTextureSize(64, 64),
                        presentationTimestampNanos = index * 33_333_333L,
                        splitMode = false,
                    )
                )
            }
            recorder.stop()
            GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
        }

        val validation = EncodedOutputValidator().validate(outputFile)
        assertTrue(validation.errors.joinToString(), validation.isValid)
    }

    @Test
    fun encodesSplitProcessedGlFramesToValidMp4() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputFile = File(context.cacheDir, "gl-processed-split-instrumented.mp4")
        outputFile.delete()

        OffscreenGlContext(GlTextureSize(64, 64)).use {
            val rawTextureId = createTexture(64, 64)
            val processedTextureId = createTexture(64, 64)
            val recorder = GlProcessedMp4Recorder(outputFile, bitrate = 600_000, frameRate = 30)
            repeat(5) { index ->
                updateTexture(rawTextureId, 64, 64, index)
                updateTexture(processedTextureId, 64, 64, index + 20)
                recorder.record(
                    ProcessedGlFrame(
                        textureId = processedTextureId,
                        rawTextureId = rawTextureId,
                        size = GlTextureSize(64, 64),
                        presentationTimestampNanos = index * 33_333_333L,
                        splitMode = true,
                    )
                )
            }
            recorder.stop()
            GLES30.glDeleteTextures(2, intArrayOf(rawTextureId, processedTextureId), 0)
        }

        val validation = EncodedOutputValidator().validate(outputFile)
        assertTrue(validation.errors.joinToString(), validation.isValid)
        assertTrue("split encoded video should not be tiny", outputFile.length() > 512L)
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
                pixels[offset] = ((x + frameIndex * 7) and 0xFF).toByte()
                pixels[offset + 1] = ((y + frameIndex * 11) and 0xFF).toByte()
                pixels[offset + 2] = (128 + frameIndex * 12).coerceAtMost(255).toByte()
                pixels[offset + 3] = 255.toByte()
            }
        }
        val buffer = java.nio.ByteBuffer.allocateDirect(pixels.size).put(pixels).apply { position(0) }
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
}
