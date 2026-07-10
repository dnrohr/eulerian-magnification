package com.dnrohr.eulerianmagnification.analysis

import java.io.File

data class SampleVideoSpec(
    val id: String,
    val displayName: String,
    val relativePath: String,
    val sourceUrl: String?,
    val sha256: String,
    val recommendedMode: MagnificationMode,
    val recommendedViewMode: ViewMode,
    val redistribution: String,
    val expectedUse: String,
) {
    fun localFile(root: File): File = File(root, relativePath)

    fun isAvailable(root: File): Boolean = localFile(root).isFile
}

object SampleVideoCatalog {
    val samples: List<SampleVideoSpec> = listOf(
        SampleVideoSpec(
            id = "mit-baby",
            displayName = "MIT Baby",
            relativePath = "sample-videos/mit-evm-baby.mp4",
            sourceUrl = "https://people.csail.mit.edu/mrub/evm/video/baby.mp4",
            sha256 = "2C5E744384AB88FCCD3AA4883959B33EB4CDB7384C3E46E788CEDE821B2478EE",
            recommendedMode = MagnificationMode.Breathing,
            recommendedViewMode = ViewMode.Split,
            redistribution = "Public MIT CSAIL reference media; download locally and do not commit to this repo.",
            expectedUse = "Slow motion / breathing-like EVM sanity sample.",
        ),
        SampleVideoSpec(
            id = "local-euler",
            displayName = "Local Euler",
            relativePath = "sample-videos/euler.mp4",
            sourceUrl = null,
            sha256 = "BF549FEAA994104817A6AFCC39037FB80A013D4074E0AC00EC167F4471B0ACBF",
            recommendedMode = MagnificationMode.Pulse,
            recommendedViewMode = ViewMode.Split,
            redistribution = "Local user-provided sample with undocumented provenance; keep local only.",
            expectedUse = "Qualitative app regression sample.",
        ),
    )

    fun byId(id: String): SampleVideoSpec? {
        return samples.firstOrNull { it.id == id }
    }

    fun availableSamples(root: File): List<SampleVideoSpec> {
        return samples.filter { it.isAvailable(root) }
    }

    fun retrievalInstructions(sample: SampleVideoSpec): String {
        return if (sample.sourceUrl != null) {
            "Download ${sample.sourceUrl} to ${sample.relativePath} and verify SHA-256 ${sample.sha256}."
        } else {
            "Place the local sample at ${sample.relativePath} and verify SHA-256 ${sample.sha256}."
        }
    }
}
