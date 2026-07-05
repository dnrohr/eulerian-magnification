package com.dnrohr.eulerianmagnification.recording

class MonotonicFrameTimeline(
    private val minFrameIntervalNanos: Long = DEFAULT_MIN_FRAME_INTERVAL_NANOS,
) {
    private var firstSourceTimestampNanos: Long? = null
    private var lastPresentationTimestampNanos: Long? = null

    fun next(sourceTimestampNanos: Long): Long {
        val firstSource = firstSourceTimestampNanos ?: sourceTimestampNanos.also {
            firstSourceTimestampNanos = it
        }
        val sourceElapsed = (sourceTimestampNanos - firstSource).coerceAtLeast(0L)
        val previousPresentation = lastPresentationTimestampNanos
        val presentationTimestampNanos = if (previousPresentation == null) {
            0L
        } else {
            maxOf(sourceElapsed, previousPresentation + minFrameIntervalNanos)
        }
        lastPresentationTimestampNanos = presentationTimestampNanos
        return presentationTimestampNanos
    }

    companion object {
        const val DEFAULT_MIN_FRAME_INTERVAL_NANOS = 1_000_000_000L / 30L
    }
}
