package com.dnrohr.eulerianmagnification.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executor
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraOesRenderer(
    private val requestRender: () -> Unit,
    private val onStats: (GlFrameStats) -> Unit,
) : GLSurfaceView.Renderer {
    private val timer = GlFrameTimer()
    private val transformMatrix = FloatArray(16)
    private val vertexBuffer = FULLSCREEN_QUAD.toFloatBuffer()
    private val lock = Any()
    private var surfaceRequest: SurfaceRequest? = null
    private var surfaceRequestExecutor: Executor? = null
    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null
    private var program = 0
    private var texTransformLocation = -1
    private var hasNewFrame = false

    fun setSurfaceRequest(
        request: SurfaceRequest,
        executor: Executor,
    ) {
        synchronized(lock) {
            surfaceRequest?.willNotProvideSurface()
            surfaceRequest = request
            surfaceRequestExecutor = executor
        }
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = GlProgram.compileProgram(OesShaderSource.VERTEX, OesShaderSource.FRAGMENT)
        texTransformLocation = GLES30.glGetUniformLocation(program, "uTexTransform")
        oesTextureId = createOesTexture()
        surfaceTexture = SurfaceTexture(oesTextureId).apply {
            setOnFrameAvailableListener {
                hasNewFrame = true
                requestRender()
            }
        }
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        providePendingSurfaceIfReady()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        timer.beginFrame(System.nanoTime())
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        val texture = surfaceTexture
        if (texture != null && hasNewFrame) {
            texture.updateTexImage()
            texture.getTransformMatrix(transformMatrix)
            hasNewFrame = false
        }
        drawCameraTexture()
        providePendingSurfaceIfReady()
        onStats(timer.endFrame(System.nanoTime()))
    }

    fun release() {
        synchronized(lock) {
            surfaceRequest?.willNotProvideSurface()
            surfaceRequest = null
            surfaceRequestExecutor = null
        }
        cameraSurface?.release()
        cameraSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
    }

    private fun providePendingSurfaceIfReady() {
        val texture = surfaceTexture ?: return
        val request: SurfaceRequest
        val executor: Executor
        synchronized(lock) {
            request = surfaceRequest ?: return
            executor = surfaceRequestExecutor ?: return
            surfaceRequest = null
            surfaceRequestExecutor = null
        }

        texture.setDefaultBufferSize(request.resolution.width, request.resolution.height)
        val surface = Surface(texture)
        cameraSurface?.release()
        cameraSurface = surface
        request.provideSurface(surface, executor) {
            surface.release()
            if (cameraSurface === surface) {
                cameraSurface = null
            }
        }
    }

    private fun drawCameraTexture() {
        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(texTransformLocation, 1, false, transformMatrix, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        vertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, vertexBuffer)
        vertexBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, vertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GlProgram.checkNoGlError("drawCameraTexture")
    }

    private fun createOesTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GlProgram.checkNoGlError("createOesTexture")
        return textures[0]
    }

    companion object {
        private const val FLOAT_BYTES = 4
        private const val VERTEX_STRIDE_BYTES = 4 * FLOAT_BYTES
        private val FULLSCREEN_QUAD = floatArrayOf(
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 0.0f,
        )
    }
}

private fun FloatArray.toFloatBuffer() = ByteBuffer
    .allocateDirect(size * Float.SIZE_BYTES)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
        put(this@toFloatBuffer)
        position(0)
    }
