package com.dnrohr.eulerianmagnification.analysis

class TimestampTracker {
    private var lastTimestampNanos: Long? = null

    fun record(timestampNanos: Long): TimestampStatus {
        val previous = lastTimestampNanos
        lastTimestampNanos = timestampNanos
        return TimestampStatus(
            isMonotonic = previous == null || timestampNanos > previous,
            previousTimestampNanos = previous,
            currentTimestampNanos = timestampNanos,
        )
    }
}

data class TimestampStatus(
    val isMonotonic: Boolean,
    val previousTimestampNanos: Long?,
    val currentTimestampNanos: Long,
)
