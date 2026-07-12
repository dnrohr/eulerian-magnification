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
    private val onProcessedFrame: (ProcessedGlFrame) -> Unit = {},
) : GLSurfaceView.Renderer {
    private val timer = GlFrameTimer()
    private val transformMatrix = FloatArray(16)
    private val externalTextureVertexBuffer = GlFullscreenQuad.EXTERNAL_TEXTURE.toFloatBuffer()
    private val framebufferTextureVertexBuffer = GlFullscreenQuad.FRAMEBUFFER_TEXTURE.toFloatBuffer()
    private val lock = Any()
    private var surfaceRequest: SurfaceRequest? = null
    private var surfaceRequestExecutor: Executor? = null
    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraSurface: Surface? = null
    private var oesProgram = 0
    private var rgbProgram = 0
    private var colorProgram = 0
    private var downsampleProgram = 0
    private var temporalProgram = 0
    private var reconstructProgram = 0
    private var oesTexTransformLocation = -1
    private var rgbInputTextureLocation = -1
    private var colorInputTextureLocation = -1
    private var colorRoiLocation = -1
    private var colorSignalLocation = -1
    private var colorDifferenceModeLocation = -1
    private var colorFullFrameModeLocation = -1
    private var downsampleInputTextureLocation = -1
    private var downsampleTexelSizeLocation = -1
    private var temporalCurrentTextureLocation = -1
    private var temporalPreviousLowTextureLocation = -1
    private var temporalPreviousHighTextureLocation = -1
    private var temporalLowAlphaLocation = -1
    private var temporalHighAlphaLocation = -1
    private var temporalInitializedLocation = -1
    private var reconstructBaseTextureLocation = -1
    private val reconstructBandpassTextureLocations = IntArray(DOWNSAMPLE_LEVELS) { -1 }
    private val reconstructLevelGainLocations = IntArray(DOWNSAMPLE_LEVELS) { -1 }
    private var reconstructAmplificationLocation = -1
    private var reconstructMaxDeltaLocation = -1
    private var reconstructDifferenceModeLocation = -1
    private var reconstructStartLevelLocation = -1
    private var rgbRenderTarget: GlRenderTarget? = null
    private var processedRenderTarget: GlRenderTarget? = null
    private var downsamplePyramid: GlPyramid? = null
    private var temporalState: GlTemporalState? = null
    private var surfaceSize = GlTextureSize(1, 1)
    private var cameraTextureSize = GlTextureSize(1, 1)
    private var lastTemporalTimestampNanos: Long? = null
    private var supportsHalfFloatTemporalTargets = false
    private var hasNewFrame = false
    private var reconstructionDiagnostics = GlReconstructionDiagnostics()
    @Volatile private var colorUniforms = ColorMagnificationUniforms(
        roi = com.dnrohr.eulerianmagnification.analysis.NormalizedRect(0.0f, 0.0f, 0.0f, 0.0f),
        amplifiedSignal = 0.0f,
        differenceMode = false,
        splitMode = false,
        fullFrameMode = false,
        presentationTimestampNanos = 0L,
        amplification = 0.0f,
        lowCutHz = 0.7,
        highCutHz = 3.0,
    )

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

    fun setColorMagnificationUniforms(uniforms: ColorMagnificationUniforms) {
        colorUniforms = uniforms
        requestRender()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        oesProgram = GlProgram.compileProgram(OesShaderSource.VERTEX, OesShaderSource.FRAGMENT)
        rgbProgram = GlProgram.compileProgram(RgbTextureShaderSource.VERTEX, RgbTextureShaderSource.FRAGMENT)
        colorProgram = GlProgram.compileProgram(ColorMagnificationShaderSource.VERTEX, ColorMagnificationShaderSource.FRAGMENT)
        supportsHalfFloatTemporalTargets = GlRenderTargetCapabilities.supportsHalfFloatColorBuffer(
            GLES30.glGetString(GLES30.GL_EXTENSIONS),
        )
        downsampleProgram = GlProgram.compileProgram(
            LivePyramidShaderSource.VERTEX,
            LivePyramidShaderSource.DOWNSAMPLE_FRAGMENT,
        )
        temporalProgram = GlProgram.compileProgram(
            LivePyramidShaderSource.VERTEX,
            LivePyramidShaderSource.TEMPORAL_BANDPASS_FRAGMENT,
        )
        reconstructProgram = GlProgram.compileProgram(
            LivePyramidShaderSource.VERTEX,
            LivePyramidShaderSource.RECONSTRUCT_FRAGMENT,
        )
        oesTexTransformLocation = GLES30.glGetUniformLocation(oesProgram, "uTexTransform")
        rgbInputTextureLocation = GLES30.glGetUniformLocation(rgbProgram, "uInputTexture")
        colorInputTextureLocation = GLES30.glGetUniformLocation(colorProgram, "uInputTexture")
        colorRoiLocation = GLES30.glGetUniformLocation(colorProgram, "uRoi")
        colorSignalLocation = GLES30.glGetUniformLocation(colorProgram, "uAmplifiedSignal")
        colorDifferenceModeLocation = GLES30.glGetUniformLocation(colorProgram, "uDifferenceMode")
        colorFullFrameModeLocation = GLES30.glGetUniformLocation(colorProgram, "uFullFrameMode")
        downsampleInputTextureLocation = GLES30.glGetUniformLocation(downsampleProgram, "uInputTexture")
        downsampleTexelSizeLocation = GLES30.glGetUniformLocation(downsampleProgram, "uTexelSize")
        temporalCurrentTextureLocation = GLES30.glGetUniformLocation(temporalProgram, "uCurrentTexture")
        temporalPreviousLowTextureLocation = GLES30.glGetUniformLocation(temporalProgram, "uPreviousLowTexture")
        temporalPreviousHighTextureLocation = GLES30.glGetUniformLocation(temporalProgram, "uPreviousHighTexture")
        temporalLowAlphaLocation = GLES30.glGetUniformLocation(temporalProgram, "uLowAlpha")
        temporalHighAlphaLocation = GLES30.glGetUniformLocation(temporalProgram, "uHighAlpha")
        temporalInitializedLocation = GLES30.glGetUniformLocation(temporalProgram, "uInitialized")
        reconstructBaseTextureLocation = GLES30.glGetUniformLocation(reconstructProgram, "uBaseTexture")
        repeat(DOWNSAMPLE_LEVELS) { index ->
            reconstructBandpassTextureLocations[index] = GLES30.glGetUniformLocation(
                reconstructProgram,
                "uBandpassTexture$index",
            )
            reconstructLevelGainLocations[index] = GLES30.glGetUniformLocation(
                reconstructProgram,
                "uLevelGain$index",
            )
        }
        reconstructAmplificationLocation = GLES30.glGetUniformLocation(reconstructProgram, "uAmplification")
        reconstructMaxDeltaLocation = GLES30.glGetUniformLocation(reconstructProgram, "uMaxDelta")
        reconstructDifferenceModeLocation = GLES30.glGetUniformLocation(reconstructProgram, "uDifferenceMode")
        reconstructStartLevelLocation = GLES30.glGetUniformLocation(reconstructProgram, "uStartLevel")
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
        surfaceSize = GlTextureSize(width, height)
        rgbRenderTarget?.release()
        downsamplePyramid?.release()
        temporalState?.release()
        processedRenderTarget?.release()
        rgbRenderTarget = GlRenderTarget(surfaceSize)
        processedRenderTarget = GlRenderTarget(surfaceSize)
        lastTemporalTimestampNanos = null
        downsamplePyramid = GlPyramid(
            baseSize = GlTextureSize(
                width = (width / 2).coerceAtLeast(1),
                height = (height / 2).coerceAtLeast(1),
            ),
            levelCount = DOWNSAMPLE_LEVELS,
        ).also { pyramid ->
            temporalState = createTemporalStateIfSupported(pyramid)
        }
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
        renderCameraTextureToRgb()
        val reconstructionRequested = liveReconstructionRequested(colorUniforms)
        val renderPath = if (renderLivePyramidReconstruction()) {
            GlRenderPath.LiveReconstruction
        } else {
            renderColorMagnification()
            if (reconstructionRequested) {
                GlRenderPath.LiveReconstructionFallback
            } else {
                GlRenderPath.ColorBridge
            }
        }
        emitProcessedFrame()
        drawOutputToScreen()
        providePendingSurfaceIfReady()
        onStats(
            timer.endFrame(
                timestampNanos = System.nanoTime(),
                renderPath = renderPath,
                reconstructionDiagnostics = reconstructionDiagnostics,
                phaseDiagnostics = colorUniforms.livePhaseDiagnostics,
            )
        )
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
        rgbRenderTarget?.release()
        rgbRenderTarget = null
        processedRenderTarget?.release()
        processedRenderTarget = null
        downsamplePyramid?.release()
        downsamplePyramid = null
        temporalState?.release()
        temporalState = null
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
        cameraTextureSize = GlTextureSize(request.resolution.width, request.resolution.height)
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

    private fun renderCameraTextureToRgb() {
        val target = rgbRenderTarget ?: return
        target.bind()
        val viewport = GlViewportLayout.aspectFill(
            surfaceSize = surfaceSize,
            contentSize = GlViewportLayout.orientContentToSurface(surfaceSize, cameraTextureSize),
        )
        GLES30.glViewport(viewport.x, viewport.y, viewport.width, viewport.height)
        GLES30.glUseProgram(oesProgram)
        GLES30.glUniformMatrix4fv(oesTexTransformLocation, 1, false, transformMatrix, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        externalTextureVertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, externalTextureVertexBuffer)
        externalTextureVertexBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, externalTextureVertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glViewport(0, 0, surfaceSize.width, surfaceSize.height)
        GlProgram.checkNoGlError("renderCameraTextureToRgb")
    }

    private fun renderColorMagnification() {
        val input = rgbRenderTarget ?: return
        val output = processedRenderTarget ?: return
        val uniforms = colorUniforms
        output.bind()
        GLES30.glUseProgram(colorProgram)
        GLES30.glUniform1i(colorInputTextureLocation, 0)
        GLES30.glUniform4f(
            colorRoiLocation,
            uniforms.roi.left,
            uniforms.roi.top,
            uniforms.roi.right,
            uniforms.roi.bottom,
        )
        GLES30.glUniform1f(colorSignalLocation, uniforms.amplifiedSignal)
        GLES30.glUniform1i(colorDifferenceModeLocation, if (uniforms.differenceMode) 1 else 0)
        GLES30.glUniform1i(colorFullFrameModeLocation, if (uniforms.fullFrameMode) 1 else 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, input.textureId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        framebufferTextureVertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, framebufferTextureVertexBuffer)
        framebufferTextureVertexBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, framebufferTextureVertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GlProgram.checkNoGlError("renderColorMagnification")
    }

    private fun renderLivePyramidReconstruction(): Boolean {
        val uniforms = colorUniforms
        if (!liveReconstructionRequested(uniforms)) {
            lastTemporalTimestampNanos = null
            reconstructionDiagnostics = GlReconstructionDiagnostics(
                fallbackReason = GlReconstructionFallbackReason.NotRequested,
            )
            return false
        }
        if (!supportsHalfFloatTemporalTargets) {
            lastTemporalTimestampNanos = null
            reconstructionDiagnostics = GlReconstructionDiagnostics(
                fallbackReason = GlReconstructionFallbackReason.HalfFloatUnsupported,
            )
            return false
        }
        val source = rgbRenderTarget ?: return missingTargets()
        val pyramid = downsamplePyramid ?: return missingTargets()
        val state = temporalState ?: return missingTargets()
        val output = processedRenderTarget ?: return missingTargets()
        if (pyramid.levels.size < DOWNSAMPLE_LEVELS || state.levels.size < DOWNSAMPLE_LEVELS) {
            reconstructionDiagnostics = GlReconstructionDiagnostics(
                activePyramidLevels = minOf(pyramid.levels.size, state.levels.size),
                internalSize = pyramid.levels.firstOrNull()?.size,
                fallbackReason = GlReconstructionFallbackReason.IncompletePyramid,
            )
            return false
        }

        return try {
            renderDownsamplePyramid(source, pyramid)
            state.swap()
            val temporalInitialized = lastTemporalTimestampNanos != null
            renderTemporalBandpass(pyramid, state, uniforms, temporalInitialized)
            renderReconstruction(source, state, output, uniforms)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GlProgram.checkNoGlError("renderLivePyramidReconstruction")
            reconstructionDiagnostics = GlReconstructionDiagnostics(
                activePyramidLevels = pyramid.levels.size,
                internalSize = pyramid.levels.first().size,
                temporalWarm = temporalInitialized,
                temporalStateLevels = state.levels.size,
                lowCutHz = uniforms.lowCutHz,
                highCutHz = uniforms.highCutHz,
                startLevel = uniforms.reconstructionProfile.startLevel,
                levelGains = (0 until DOWNSAMPLE_LEVELS).map(uniforms.reconstructionProfile.levelPolicy::gainFor),
                maxDelta = uniforms.reconstructionProfile.levelPolicy.maxDelta,
            )
            true
        } catch (_: GlException) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            supportsHalfFloatTemporalTargets = false
            lastTemporalTimestampNanos = null
            reconstructionDiagnostics = GlReconstructionDiagnostics(
                activePyramidLevels = pyramid.levels.size,
                internalSize = pyramid.levels.firstOrNull()?.size,
                fallbackReason = GlReconstructionFallbackReason.GlError,
            )
            false
        }
    }

    private fun missingTargets(): Boolean {
        reconstructionDiagnostics = GlReconstructionDiagnostics(
            fallbackReason = GlReconstructionFallbackReason.MissingRenderTargets,
        )
        return false
    }

    private fun liveReconstructionRequested(uniforms: ColorMagnificationUniforms): Boolean {
        return uniforms.fullFrameMode
    }

    private fun createTemporalStateIfSupported(pyramid: GlPyramid): GlTemporalState? {
        if (!supportsHalfFloatTemporalTargets) {
            return null
        }
        return try {
            GlTemporalState(GlTemporalStateLayout.levelSizesFor(pyramid))
        } catch (_: GlException) {
            supportsHalfFloatTemporalTargets = false
            null
        }
    }

    private fun renderDownsamplePyramid(
        source: GlRenderTarget,
        pyramid: GlPyramid,
    ) {
        var inputTextureId = source.textureId
        var inputSize = source.size
        pyramid.levels.forEach { level ->
            level.bind()
            GLES30.glUseProgram(downsampleProgram)
            GLES30.glUniform1i(downsampleInputTextureLocation, 0)
            GLES30.glUniform2f(
                downsampleTexelSizeLocation,
                1.0f / inputSize.width,
                1.0f / inputSize.height,
            )
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
            drawFramebufferQuad()
            inputTextureId = level.textureId
            inputSize = level.size
        }
    }

    private fun renderTemporalBandpass(
        pyramid: GlPyramid,
        state: GlTemporalState,
        uniforms: ColorMagnificationUniforms,
        temporalInitialized: Boolean,
    ) {
        val coefficients = TemporalBandpassCoefficients.from(
            lowCutHz = uniforms.lowCutHz,
            highCutHz = uniforms.highCutHz,
            dtSeconds = temporalDeltaSeconds(uniforms.presentationTimestampNanos),
        )
        pyramid.levels.zip(state.levels).forEach { (currentLevel, temporalLevel) ->
            temporalLevel.bindForTemporalUpdate()
            GLES30.glUseProgram(temporalProgram)
            GLES30.glUniform1i(temporalCurrentTextureLocation, 0)
            GLES30.glUniform1i(temporalPreviousLowTextureLocation, 1)
            GLES30.glUniform1i(temporalPreviousHighTextureLocation, 2)
            GLES30.glUniform1f(temporalLowAlphaLocation, coefficients.lowAlpha)
            GLES30.glUniform1f(temporalHighAlphaLocation, coefficients.highAlpha)
            GLES30.glUniform1i(temporalInitializedLocation, if (temporalInitialized) 1 else 0)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, currentLevel.textureId)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, temporalLevel.previousLowpass.textureId)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, temporalLevel.previousHighpass.textureId)
            drawFramebufferQuad()
        }
    }

    private fun renderReconstruction(
        source: GlRenderTarget,
        state: GlTemporalState,
        output: GlRenderTarget,
        uniforms: ColorMagnificationUniforms,
    ) {
        output.bind()
        GLES30.glUseProgram(reconstructProgram)
        GLES30.glUniform1i(reconstructBaseTextureLocation, 0)
        repeat(DOWNSAMPLE_LEVELS) { index ->
            GLES30.glUniform1i(reconstructBandpassTextureLocations[index], index + 1)
        }
        GLES30.glUniform1f(reconstructAmplificationLocation, uniforms.reconstructionAmplification)
        repeat(DOWNSAMPLE_LEVELS) { index ->
            GLES30.glUniform1f(
                reconstructLevelGainLocations[index],
                uniforms.reconstructionProfile.levelPolicy.gainFor(index),
            )
        }
        GLES30.glUniform1f(reconstructMaxDeltaLocation, uniforms.reconstructionProfile.levelPolicy.maxDelta)
        GLES30.glUniform1i(reconstructDifferenceModeLocation, if (uniforms.differenceMode) 1 else 0)
        GLES30.glUniform1i(reconstructStartLevelLocation, uniforms.reconstructionProfile.startLevel)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, source.textureId)
        repeat(DOWNSAMPLE_LEVELS) { index ->
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1 + index)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, state.levels[index].bandpass.textureId)
        }
        drawFramebufferQuad()
    }

    private fun temporalDeltaSeconds(timestampNanos: Long): Double {
        val previousTimestamp = lastTemporalTimestampNanos
        lastTemporalTimestampNanos = timestampNanos.takeIf { it > 0L }
        val deltaNanos = if (timestampNanos > 0L && previousTimestamp != null) {
            timestampNanos - previousTimestamp
        } else {
            DEFAULT_FRAME_NANOS
        }
        return deltaNanos.coerceAtLeast(1L) / NANOS_PER_SECOND
    }

    private fun drawOutputToScreen() {
        val rawTarget = rgbRenderTarget ?: return
        val processedTarget = processedRenderTarget ?: rawTarget
        if (colorUniforms.splitMode) {
            val (left, right) = GlViewportLayout.splitHorizontal(surfaceSize)
            drawTextureToScreen(rawTarget, left)
            drawTextureToScreen(processedTarget, right)
        } else {
            drawTextureToScreen(processedTarget, GlViewportLayout.full(surfaceSize))
        }
    }

    private fun emitProcessedFrame() {
        val processedTarget = processedRenderTarget ?: return
        onProcessedFrame(
            ProcessedGlFrame(
                textureId = processedTarget.textureId,
                size = processedTarget.size,
                presentationTimestampNanos = colorUniforms.presentationTimestampNanos,
                splitMode = colorUniforms.splitMode,
            )
        )
    }

    private fun drawTextureToScreen(target: GlRenderTarget, viewport: GlViewport) {
        GLES30.glViewport(viewport.x, viewport.y, viewport.width, viewport.height)
        GLES30.glUseProgram(rgbProgram)
        GLES30.glUniform1i(rgbInputTextureLocation, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, target.textureId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        framebufferTextureVertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, framebufferTextureVertexBuffer)
        framebufferTextureVertexBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, framebufferTextureVertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
        GlProgram.checkNoGlError("drawRgbTextureToScreen")
    }

    private fun drawFramebufferQuad() {
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        framebufferTextureVertexBuffer.position(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, framebufferTextureVertexBuffer)
        framebufferTextureVertexBuffer.position(2)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, VERTEX_STRIDE_BYTES, framebufferTextureVertexBuffer)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
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
        private const val DOWNSAMPLE_LEVELS = 3
        private const val DEFAULT_FRAME_NANOS = 33_333_333L
        private const val NANOS_PER_SECOND = 1_000_000_000.0
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
