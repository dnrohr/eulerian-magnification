package com.dnrohr.eulerianmagnification.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

class GlDebugRenderer(
    private val onStats: (GlFrameStats) -> Unit,
) : GLSurfaceView.Renderer {
    private val timer = GlFrameTimer()
    private val vertexBuffer = FULLSCREEN_TRIANGLE.toFloatBuffer()
    private var program = 0
    private var frameIndex = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = GlProgram.compileProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        GLES30.glClearColor(0.02f, 0.03f, 0.04f, 0.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        timer.beginFrame(System.nanoTime())
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(program)
        val pulse = (sin(frameIndex++ / 18.0) * 0.5 + 0.5).toFloat()
        GLES30.glUniform1f(GLES30.glGetUniformLocation(program, "uPulse"), pulse)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
        GLES30.glDisableVertexAttribArray(0)
        onStats(timer.endFrame(System.nanoTime()))
    }

    companion object {
        private val FULLSCREEN_TRIANGLE = floatArrayOf(
            -1.0f, -1.0f,
            3.0f, -1.0f,
            -1.0f, 3.0f,
        )

        private const val VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec2 aPosition;
            out vec2 vPosition;

            void main() {
                vPosition = aPosition;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """#version 300 es
            precision mediump float;
            uniform float uPulse;
            in vec2 vPosition;
            out vec4 outColor;

            void main() {
                vec2 uv = vPosition * 0.5 + 0.5;
                outColor = vec4(0.02 + uv.x * 0.08, 0.10 + uPulse * 0.18, 0.14 + uv.y * 0.10, 0.22);
            }
        """
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
