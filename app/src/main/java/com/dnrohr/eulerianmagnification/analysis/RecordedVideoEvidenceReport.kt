package com.dnrohr.eulerianmagnification.analysis

import java.util.Locale
import kotlin.math.abs

object RecordedVideoEvidenceReport {
    fun toHtml(
        sourceName: String,
        settings: AnalysisSettings,
        frames: List<RecordedVideoProcessedFrame>,
        qualitySummary: String,
    ): String {
        val samples = frames.map { it.sample }
        val maxMagnitude = samples.maxOfOrNull { abs(it.bandpassedGreen) }?.coerceAtLeast(0.001) ?: 0.001
        val points = samples.mapIndexed { index, sample ->
            val x = if (samples.size <= 1) 0.0 else index * PLOT_WIDTH / (samples.size - 1).toDouble()
            val y = PLOT_MIDLINE - (sample.bandpassedGreen / maxMagnitude) * PLOT_AMPLITUDE
            "${format(x)},${format(y.coerceIn(0.0, PLOT_HEIGHT))}"
        }.joinToString(" ")
        val averageFps = samples
            .map { it.analysisFps }
            .filter { it > 0.0 }
            .average()
            .takeUnless { it.isNaN() }
            ?: 0.0

        return """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8">
              <title>Eulerian Magnification Evidence Report</title>
              <style>
                body { font-family: sans-serif; margin: 24px; background: #101418; color: #eef4f8; }
                .meta { color: #c8d3dc; }
                svg { width: 100%; max-width: 900px; height: auto; background: #06080a; border: 1px solid #34424d; }
                .axis { stroke: #34424d; stroke-width: 1; }
                .signal { fill: none; stroke: #ff6b6b; stroke-width: 2; }
              </style>
            </head>
            <body>
              <h1>Evidence Report</h1>
              <p class="meta">Source: ${sourceName.escapeHtml()}</p>
              <p class="meta">Mode: ${settings.mode.label.escapeHtml()} / ${settings.viewMode.label.escapeHtml()} / ${format(settings.lowCutHz)}-${format(settings.highCutHz)} Hz / ${format(settings.amplification.toDouble())}x</p>
              <p class="meta">Frames: ${frames.size} / Average FPS: ${format(averageFps)} / Quality: ${qualitySummary.escapeHtml()}</p>
              <svg viewBox="0 0 ${PLOT_WIDTH.toInt()} ${PLOT_HEIGHT.toInt()}" role="img" aria-label="Bandpassed signal over time">
                <line class="axis" x1="0" y1="${PLOT_MIDLINE.toInt()}" x2="${PLOT_WIDTH.toInt()}" y2="${PLOT_MIDLINE.toInt()}"></line>
                <polyline class="signal" points="$points"></polyline>
              </svg>
            </body>
            </html>
        """.trimIndent()
    }

    private fun format(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun String.escapeHtml(): String {
        return replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private const val PLOT_WIDTH = 900.0
    private const val PLOT_HEIGHT = 240.0
    private const val PLOT_MIDLINE = PLOT_HEIGHT / 2.0
    private const val PLOT_AMPLITUDE = 100.0
}
