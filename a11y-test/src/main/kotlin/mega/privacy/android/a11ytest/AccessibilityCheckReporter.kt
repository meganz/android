package mega.privacy.android.a11ytest

import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.ResultMetadata
import com.google.android.apps.common.testing.accessibility.framework.checks.ImageContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.CheckResultsCallback
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * A single accessibility check result captured for reporting.
 *
 * @property checkName simple name of the accessibility check that produced the result
 * @property resultType the result type name, e.g. `ERROR`, `WARNING` or `SUPPRESSED`
 * @property elementDescription class name, resource name and screen bounds of the evaluated
 * element, when available
 * @property message human readable description of the finding, when available
 * @property metadata measured values backing the result, e.g. contrast ratios or touch target
 * width and height
 */
data class AccessibilityCheckReportEntry(
    val checkName: String,
    val resultType: String,
    val elementDescription: String?,
    val message: String?,
    val metadata: Map<String, Any?>,
)

/**
 * Records every accessibility check result delivered by an [AccessibilityValidator] — including
 * suppressed design-owned findings — and serializes each batch to a JSON report file.
 *
 * @param reportName prefix for the generated report file names, typically the test class name
 * @param reportWriter destination for the JSON reports
 */
class AccessibilityCheckReporter(
    private val reportName: String = "a11y-check-results",
    private val reportWriter: AccessibilityReportWriter = TestStorageReportWriter(),
) : AccessibilityValidator.CheckResultsListener {

    private val recordedEntries = CopyOnWriteArrayList<AccessibilityCheckReportEntry>()
    private val reportCounter = AtomicInteger()

    /**
     * Every check result recorded so far.
     */
    val entries: List<AccessibilityCheckReportEntry> get() = recordedEntries.toList()

    override fun onResults(callback: CheckResultsCallback) {
        val batch = callback.accessibilityViewCheckResults.map { it.toReportEntry() }
        recordedEntries += batch
        val report = JSONObject().apply {
            put("screenshotPath", callback.screenshotPath ?: JSONObject.NULL)
            put("results", JSONArray(batch.map { it.toJson() }))
        }
        // The timestamp keeps file names unique across reporter instances, which restart the
        // counter for every test.
        reportWriter.write(
            "a11y/$reportName-${System.currentTimeMillis()}-${reportCounter.incrementAndGet()}.json",
            report.toString(2),
        )
    }
}

private fun AccessibilityViewCheckResult.toReportEntry() = AccessibilityCheckReportEntry(
    checkName = sourceCheckClass.simpleName,
    resultType = type.name,
    elementDescription = describeElement(),
    message = runCatching { getMessage(Locale.ENGLISH)?.toString() }.getOrNull(),
    metadata = metadata?.toPlainMap().orEmpty(),
)

private fun AccessibilityViewCheckResult.describeElement(): String? {
    val element: ViewHierarchyElement = element ?: return null
    return listOfNotNull(
        element.className?.toString(),
        element.resourceName,
        element.boundsInScreen?.toString(),
    ).joinToString(" ").ifBlank { null }
}

private fun AccessibilityCheckReportEntry.toJson() = JSONObject().apply {
    put("check", checkName)
    put("type", resultType)
    put("element", elementDescription ?: JSONObject.NULL)
    put("message", message ?: JSONObject.NULL)
    put("metadata", JSONObject().apply {
        metadata.forEach { (key, value) -> put(key, JSONObject.wrap(value) ?: JSONObject.NULL) }
    })
}

// ResultMetadata exposes no key iteration, so the measured values of the checks the policy
// cares about are probed by key instead.
private val reportedMetadataKeys = listOf(
    TextContrastCheck.KEY_CONTRAST_RATIO,
    TextContrastCheck.KEY_REQUIRED_CONTRAST_RATIO,
    TextContrastCheck.KEY_TEXT_COLOR,
    TextContrastCheck.KEY_FOREGROUND_COLOR,
    TextContrastCheck.KEY_BACKGROUND_COLOR,
    TextContrastCheck.KEY_SCREENSHOT_BOUNDS_STRING,
    TextContrastCheck.KEY_VIEW_BOUNDS_STRING,
    TextContrastCheck.KEY_IS_LARGE_TEXT,
    ImageContrastCheck.KEY_CONTRAST_RATIO,
    ImageContrastCheck.KEY_SCREENSHOT_BOUNDS_STRING,
    ImageContrastCheck.KEY_VIEW_BOUNDS_STRING,
    TextSizeCheck.KEY_TEXT_SIZE_UNIT,
    TextSizeCheck.KEY_ESTIMATED_TEXT_SIZE_DP,
    TouchTargetSizeCheck.KEY_WIDTH,
    TouchTargetSizeCheck.KEY_HEIGHT,
    TouchTargetSizeCheck.KEY_REQUIRED_WIDTH,
    TouchTargetSizeCheck.KEY_REQUIRED_HEIGHT,
).distinct()

private fun ResultMetadata.toPlainMap(): Map<String, Any?> =
    reportedMetadataKeys
        .filter { containsKey(it) }
        .associateWith { key -> valueOf(key) }

private fun ResultMetadata.valueOf(key: String): Any? {
    // The typed getters throw on a type mismatch, so probing them in turn recovers the value
    // without knowing the type up front.
    val typedGetters = listOf<(String) -> Any?>(
        { getDouble(it) },
        { getFloat(it) },
        { getInt(it) },
        { getLong(it) },
        { getBoolean(it) },
        { getString(it) },
    )
    return typedGetters.firstNotNullOfOrNull { getter -> runCatching { getter(key) }.getOrNull() }
}
