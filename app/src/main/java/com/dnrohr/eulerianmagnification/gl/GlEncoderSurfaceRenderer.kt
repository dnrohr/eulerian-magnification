package com.dnrohr.eulerianmagnification.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.EGLExt
import android.opengl.GLES30
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GlEncoderSurfaceConfig {
    const val EGL_RECORDABLE_ANDROID = 0x3142
    const val EGL_OPENGL_ES3_BIT_KHR = 0x00000040

    fun configAttributes(): IntArray {
        return intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE,
        )
    }

    fun contextAttributes(): IntArray {
        return intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
            EGL14.EGL_NONE,
        )
    }
}

class GlEncoderSurfaceRenderer(
    private val inputSurface: Surface,
) {
    private val vertexBuffer = GlFullscreenQuad.FRAMEBUFFER_TEXTURE.toEncoderFloatBuffer()
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var rgbProgram = 0
    private var rgbInputTextureLocation = -1
    private var released = false

    fun initialize(sharedContext: EGLContext = EGL14.eglGetCurrentContext()) {
        check(eglDisplay == EGL14.EGL_NO_DISPLAY) { "Encoder EGL renderer is already initialized." }
        check(sharedContext != EGL14.EGL_NO_CONTEXT) { "A current shared EGL context is required." }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Could not get EGL display." }

        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "Could not initialize EGL." }

        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)
        check(
            EGL14.eglChooseConfig(
                eglDisplay,
                GlEncoderSurfaceConfig.configAttributes(),
                0,
                configs,
                0,
                configs.size,
                configCount,
                0,
            ) && configCount[0] > 0
        ) { "Could not choose recordable EGL config." }

        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            configs[0],
            sharedContext,
            GlEncoderSurfaceConfig.contextAttributes(),
            0,
        )
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "Could not create encoder EGL context." }

        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            configs[0],
            inputSurface,
            intArrayOf(EGL14.EGL_NONE),
            0,
        )
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "Could not create encoder EGL window surface." }
    }

    fun render(frame: ProcessedGlFrame) {
        check(!released) { "Encoder EGL renderer has been released." }
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Encoder EGL renderer is not initialized." }

        val previous = CurrentEglState.capture()
        try {
            check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                "Could not make encoder EGL context current."
            }
            ensureProgram()
            GLES30.glViewport(0, 0, frame.size.width, frame.size.height)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            drawTexture(frame.textureId)
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, frame.presentationTimestampNanos)
            check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) { "Could not swap encoder EGL buffers." }
        } finally {
            previous.restore()
        }
    }

    fun release() {
        if (released) return
        released = true
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        inputSurface.release()
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    private fun ensureProgram() {
        if (rgbProgram != 0) return
        rgbProgram = GlProgram.compileProgram(RgbTextureShaderSource.VERTEX, RgbTextureShaderSource.FRAGMENT)
        rgbInputTextureLocation = GLES30.glGetUniformLocation(rgbProgram, "uInputTexture")
    }

    private fun drawTexture(textureId: Int) {
        GLES30.glUseProgram(rgbProgram)
        GLES30.glUniform1i(rgbInputTextureLocation, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, vertexBuffer)
        vertexBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, vertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GlProgram.checkNoGlError("GlEncoderSurfaceRenderer.drawTexture")
    }

    private data class CurrentEglState(
        val display: EGLDisplay,
        val drawSurface: EGLSurface,
        val readSurface: EGLSurface,
        val context: EGLContext,
    ) {
        fun restore() {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, drawSurface, readSurface, context)
            }
        }

        companion object {
            fun capture(): CurrentEglState {
                return CurrentEglState(
                    display = EGL14.eglGetCurrentDisplay(),
                    drawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),
                    readSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ),
                    context = EGL14.eglGetCurrentContext(),
                )
            }
        }
    }

    companion object {
        private const val FLOAT_BYTES = 4
        private const val VERTEX_STRIDE_BYTES = 4 * FLOAT_BYTES
    }
}

private fun FloatArray.toEncoderFloatBuffer() = ByteBuffer
    .allocateDirect(size * Float.SIZE_BYTES)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
        put(this@toEncoderFloatBuffer)
        position(0)
    }
