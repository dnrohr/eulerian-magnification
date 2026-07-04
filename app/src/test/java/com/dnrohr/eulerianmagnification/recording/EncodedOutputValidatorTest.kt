package com.dnrohr.eulerianmagnification.recording

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class EncodedOutputValidatorTest {
    @Test
    fun rejectsMissingOutput() {
        val file = Files.createTempDirectory("encoded-output").resolve("missing.mp4").toFile()

        val result = EncodedOutputValidator().validate(file)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("does not exist") })
    }

    @Test
    fun rejectsEmptyOutput() {
        val file = Files.createTempFile("encoded-output", ".mp4").toFile()

        val result = EncodedOutputValidator().validate(file)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("empty") })
    }

    @Test
    fun acceptsNonEmptyMp4ContainerCandidate() {
        val file = Files.createTempFile("encoded-output", ".mp4").toFile()
        file.writeBytes(byteArrayOf(0, 0, 0, 24, 102, 116, 121, 112))

        val result = EncodedOutputValidator().validate(file)

        assertTrue(result.isValid)
    }
}
