package com.dnrohr.eulerianmagnification.recording

import java.io.File
import java.io.RandomAccessFile

class EncodedOutputValidator {
    fun validate(file: File): EncodedOutputValidation {
        val errors = mutableListOf<String>()
        errors += buildList {
            if (!file.exists()) add("Output file does not exist.")
            if (file.exists() && file.length() == 0L) add("Output file is empty.")
            if (!file.name.endsWith(".mp4", ignoreCase = true)) add("Output file must use the .mp4 extension.")
        }

        if (errors.isEmpty()) {
            errors += validateMp4Atoms(file)
        }

        return EncodedOutputValidation(
            isValid = errors.isEmpty(),
            errors = errors,
        )
    }

    private fun validateMp4Atoms(file: File): List<String> {
        val atoms = readTopLevelAtoms(file)
        return buildList {
            if ("ftyp" !in atoms) add("MP4 file is missing the ftyp atom.")
            if ("moov" !in atoms) add("MP4 file is missing the moov atom.")
            if ("mdat" !in atoms) add("MP4 file is missing the mdat atom.")
        }
    }

    private fun readTopLevelAtoms(file: File): Set<String> {
        val atoms = mutableSetOf<String>()
        RandomAccessFile(file, "r").use { input ->
            while (input.filePointer + ATOM_HEADER_BYTES <= input.length()) {
                val atomStart = input.filePointer
                val size = input.readInt().toLong() and UINT_MASK
                val typeBytes = ByteArray(4)
                input.readFully(typeBytes)
                atoms += typeBytes.decodeToString()

                val nextAtom = when {
                    size == 1L && input.filePointer + 8L <= input.length() -> {
                        val extendedSize = input.readLong()
                        atomStart + extendedSize
                    }
                    size >= ATOM_HEADER_BYTES -> atomStart + size
                    else -> input.length()
                }

                if (nextAtom <= input.filePointer || nextAtom > input.length()) {
                    break
                }
                input.seek(nextAtom)
            }
        }
        return atoms
    }

    companion object {
        private const val ATOM_HEADER_BYTES = 8L
        private const val UINT_MASK = 0xFFFFFFFFL
    }
}

data class EncodedOutputValidation(
    val isValid: Boolean,
    val errors: List<String>,
)
