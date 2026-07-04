package com.dnrohr.eulerianmagnification.capabilities

import android.content.Context
import android.util.Log
import java.io.File

class CapabilityReportStore(private val context: Context) {
    fun writeLatestReport(reporter: CapabilityReporter = CapabilityReporter(context)): File {
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val directory = File(root, "capabilities")
        directory.mkdirs()
        val output = File(directory, REPORT_FILE_NAME)
        output.writeText(reporter.buildReport().toString(2))
        Log.i(TAG, "Wrote capability report to ${output.absolutePath}")
        return output
    }

    companion object {
        const val REPORT_FILE_NAME = "pixel8a_camera_capabilities.json"
        private const val TAG = "CapabilityReportStore"
    }
}
