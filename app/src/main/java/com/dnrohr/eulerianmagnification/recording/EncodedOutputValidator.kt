package com.dnrohr.eulerianmagnification.recording

import java.io.File

class EncodedOutputValidator {
    fun validate(file: File): EncodedOutputValidation {
        val errors = buildList {
            if (!file.exists()) add("Output file does not exist.")
            if (file.exists() && file.length() == 0L) add("Output file is empty.")
            if (!file.name.endsWith(".mp4", ignoreCase = true)) add("Output file must use the .mp4 extension.")
        }

        return EncodedOutputValidation(
            isValid = errors.isEmpty(),
            errors = errors,
        )
    }
}

data class EncodedOutputValidation(
    val isValid: Boolean,
    val errors: List<String>,
)
