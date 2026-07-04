package com.dnrohr.eulerianmagnification.profiling

class FpsMeter(private val windowSize: Int = 30) {
    private val frameTimesNanos = ArrayDeque<Long>()

    fun recordFrame(timestampNanos: Long) {
        frameTimesNanos.addLast(timestampNanos)
        while (frameTimesNanos.size > windowSize) {
            frameTimesNanos.removeFirst()
        }
    }

    fun framesPerSecond(): Double {
        if (frameTimesNanos.size < 2) return 0.0
        val elapsedNanos = frameTimesNanos.last() - frameTimesNanos.first()
        if (elapsedNanos <= 0L) return 0.0
        return (frameTimesNanos.size - 1) * NANOS_PER_SECOND / elapsedNanos.toDouble()
    }

    companion object {
        private const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
