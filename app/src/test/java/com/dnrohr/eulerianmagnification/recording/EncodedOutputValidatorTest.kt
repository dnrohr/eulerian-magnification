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
        file.writeBytes(
            mp4Atom("ftyp", byteArrayOf(105, 115, 111, 109, 0, 0, 0, 1)) +
                mp4Atom("mdat", byteArrayOf(1, 2, 3, 4)) +
                mp4Atom("moov", byteArrayOf(5, 6, 7, 8)),
        )

        val result = EncodedOutputValidator().validate(file)

        assertTrue(result.isValid)
    }

    @Test
    fun rejectsMp4CandidateMissingRequiredAtoms() {
        val file = Files.createTempFile("encoded-output", ".mp4").toFile()
        file.writeBytes(mp4Atom("ftyp", byteArrayOf(105, 115, 111, 109)))

        val result = EncodedOutputValidator().validate(file)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("moov") })
        assertTrue(result.errors.any { it.contains("mdat") })
    }

    private fun mp4Atom(type: String, payload: ByteArray): ByteArray {
        val size = 8 + payload.size
        return byteArrayOf(
            ((size ushr 24) and 0xFF).toByte(),
            ((size ushr 16) and 0xFF).toByte(),
            ((size ushr 8) and 0xFF).toByte(),
            (size and 0xFF).toByte(),
        ) + type.encodeToByteArray() + payload
    }
}
