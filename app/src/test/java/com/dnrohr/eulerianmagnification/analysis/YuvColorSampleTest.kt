package com.dnrohr.eulerianmagnification.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YuvColorSampleTest {
    @Test
    fun convertsNeutralYuvToGray() {
        val sample = YuvColorSample.fromYuv(y = 128.0, u = 128.0, v = 128.0)

        assertEquals(128.0, sample.red, 0.0001)
        assertEquals(128.0, sample.green, 0.0001)
        assertEquals(128.0, sample.blue, 0.0001)
        assertFalse(sample.saturated)
    }

    @Test
    fun flagsClippedBrightOrDarkChannelsAsSaturated() {
        assertTrue(YuvColorSample.fromYuv(y = 255.0, u = 128.0, v = 128.0).saturated)
        assertTrue(YuvColorSample.fromYuv(y = 0.0, u = 128.0, v = 128.0).saturated)
    }

    @Test
    fun flagsChromaDrivenChannelClipping() {
        val sample = YuvColorSample.fromYuv(y = 128.0, u = 255.0, v = 255.0)

        assertTrue(sample.red >= 251.0 || sample.blue >= 251.0)
        assertTrue(sample.saturated)
    }
}
