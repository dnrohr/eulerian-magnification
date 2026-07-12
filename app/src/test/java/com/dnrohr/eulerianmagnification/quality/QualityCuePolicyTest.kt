package com.dnrohr.eulerianmagnification.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityCuePolicyTest {
    @Test
    fun doesNotCueWhenDisabled() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.Good),
            currentStatuses = listOf(QualityStatus.TooDark),
            state = QualityCueState(),
            nowMillis = 10_000L,
            enabled = false,
            systemHapticsAllowed = true,
        )

        assertFalse(decision.shouldCue)
    }

    @Test
    fun doesNotCueWhenSystemHapticsAreDisabled() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.Good),
            currentStatuses = listOf(QualityStatus.TooDark),
            state = QualityCueState(),
            nowMillis = 10_000L,
            enabled = true,
            systemHapticsAllowed = false,
        )

        assertFalse(decision.shouldCue)
    }

    @Test
    fun cuesWhenQualityRegressesFromGood() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.Good),
            currentStatuses = listOf(QualityStatus.LightingFlicker),
            state = QualityCueState(),
            nowMillis = 10_000L,
            enabled = true,
            systemHapticsAllowed = true,
        )

        assertTrue(decision.shouldCue)
        assertEquals(QualityCueReason.QualityRegressed, decision.reason)
        assertEquals(QualityStatus.LightingFlicker, decision.nextState.lastCueStatus)
    }

    @Test
    fun cuesWhenQualityRecoversToGood() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.CameraMotion),
            currentStatuses = listOf(QualityStatus.Good),
            state = QualityCueState(),
            nowMillis = 10_000L,
            enabled = true,
            systemHapticsAllowed = true,
        )

        assertTrue(decision.shouldCue)
        assertEquals(QualityCueReason.QualityRecovered, decision.reason)
    }

    @Test
    fun cuesWhenPrimaryIssueChanges() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.LowFps),
            currentStatuses = listOf(QualityStatus.TimingUnstable),
            state = QualityCueState(),
            nowMillis = 10_000L,
            enabled = true,
            systemHapticsAllowed = true,
        )

        assertTrue(decision.shouldCue)
        assertEquals(QualityCueReason.PrimaryIssueChanged, decision.reason)
    }

    @Test
    fun rateLimitsRepeatedCues() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.Good),
            currentStatuses = listOf(QualityStatus.TooDark),
            state = QualityCueState(
                lastCueAtMillis = 5_000L,
                lastCueStatus = QualityStatus.LowFps,
            ),
            nowMillis = 9_000L,
            enabled = true,
            systemHapticsAllowed = true,
        )

        assertFalse(decision.shouldCue)
    }

    @Test
    fun ignoresNoisySignalOnlyChanges() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.Good),
            currentStatuses = listOf(QualityStatus.SignalWeak),
            state = QualityCueState(),
            nowMillis = 10_000L,
            enabled = true,
            systemHapticsAllowed = true,
        )

        assertFalse(decision.shouldCue)
    }

    @Test
    fun cuesWhenFullFrameAnalysisFallsBehind() {
        val decision = QualityCuePolicy.decide(
            previousStatuses = listOf(QualityStatus.Good),
            currentStatuses = listOf(QualityStatus.FullFrameSlow),
            state = QualityCueState(),
            nowMillis = 10_000L,
            enabled = true,
            systemHapticsAllowed = true,
        )

        assertTrue(decision.shouldCue)
        assertEquals(QualityCueReason.QualityRegressed, decision.reason)
    }
}
