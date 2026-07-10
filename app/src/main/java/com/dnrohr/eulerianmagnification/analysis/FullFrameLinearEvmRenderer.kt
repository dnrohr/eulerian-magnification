package com.dnrohr.eulerianmagnification.analysis

class FullFrameLinearEvmRenderer(
    private val settings: AnalysisSettings,
    levelCount: Int = DEFAULT_LEVEL_COUNT,
) {
    private val pyramidBuilder = RgbFramePyramidBuilder(levelCount)
    private val temporalBandpass = RgbPyramidTemporalBandpass(settings)
    private val reconstructor = RgbPyramidReconstructor()

    fun render(frame: RgbFrame): RgbFrame {
        val pyramid = pyramidBuilder.build(frame)
        val bandpass = temporalBandpass.update(pyramid)
        return reconstructor.reconstruct(
            base = frame,
            bandpass = bandpass,
            amplification = settings.amplification,
            startLevel = startLevelFor(settings.mode),
        )
    }

    private fun startLevelFor(mode: MagnificationMode): Int {
        return when (mode) {
            MagnificationMode.Pulse -> 0
            MagnificationMode.Breathing,
            MagnificationMode.Tremor,
            MagnificationMode.ObjectVibration,
            -> 1
        }
    }

    companion object {
        private const val DEFAULT_LEVEL_COUNT = 4
    }
}
