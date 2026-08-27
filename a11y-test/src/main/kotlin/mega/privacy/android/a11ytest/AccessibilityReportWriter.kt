package mega.privacy.android.a11ytest

import androidx.test.services.storage.TestStorage

/**
 * Persists accessibility check reports so they can be collected from the host after an
 * instrumented run.
 */
fun interface AccessibilityReportWriter {

    /**
     * Writes [content] to a file at the given device-relative [path].
     */
    fun write(path: String, content: String)
}

/**
 * Writes reports through the androidx Test Storage service, which Gradle pulls into the module's
 * build output directory after a managed-device or connected test run.
 */
internal class TestStorageReportWriter : AccessibilityReportWriter {

    private val testStorage = TestStorage()

    override fun write(path: String, content: String) {
        testStorage.openOutputFile(path).use { it.write(content.toByteArray()) }
    }
}
