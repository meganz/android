package mega.privacy.android.a11ytest

import androidx.test.platform.app.InstrumentationRegistry

/**
 * What a run of the accessibility suite enforces and what it reports on.
 *
 * The suite feeds two CI jobs that answer different questions: a per-merge-request gate that
 * fails on developer-actionable findings, and a weekly run that reports design-owned findings to
 * the people who own them. The accessibility test framework has no per-check enable switch —
 * `setCheckPreset` selects a whole preset — so the split is expressed with the three levers a
 * validator does expose: what it throws on, what its listener records, and whether screenshots
 * are captured. Screenshot capture is what lets the pixel-measuring contrast checks produce a
 * measured result at all, and it is the expensive part of a run.
 *
 * @property failsOnGateFindings whether a developer-owned finding throws and fails the test
 * @property capturesScreenshots whether each check captures the screenshot the contrast checks
 * measure against
 * @property recordsDesignFindings whether design-owned findings reach the reporter and the JSON
 * report
 * @property themes the themes a sweep is checked in
 */
enum class AccessibilityRunMode(
    val failsOnGateFindings: Boolean,
    val capturesScreenshots: Boolean,
    val recordsDesignFindings: Boolean,
    val themes: List<SweepTheme>,
) {

    /**
     * Developer-actionable findings only, for the per-merge-request gate.
     *
     * Missing labels, undersized touch targets, duplicate clickable bounds and traversal order
     * are all theme-independent, so only the light pass is run: a measured dark pass produced no
     * gate finding the light pass had not already produced, and it costs a recomposition and a
     * second check pass per sweep.
     */
    Gate(
        failsOnGateFindings = true,
        capturesScreenshots = false,
        recordsDesignFindings = false,
        themes = listOf(SweepTheme.Light),
    ),

    /**
     * Design-owned findings only, for the weekly report.
     *
     * Nothing throws, so the run always reaches the end and always produces a report rather than
     * stopping at the first developer-owned finding the gate has already failed on. Both themes
     * are swept: contrast is measured from rendered pixels, so a light-only run is blind to
     * every dark-theme finding.
     */
    Report(
        failsOnGateFindings = false,
        capturesScreenshots = true,
        recordsDesignFindings = true,
        themes = SweepTheme.entries,
    ),

    /**
     * Gate enforcement and the full design report in one pass, which is what a run that selects
     * no mode gets.
     */
    GateAndReport(
        failsOnGateFindings = true,
        capturesScreenshots = true,
        recordsDesignFindings = true,
        themes = SweepTheme.entries,
    ),
    ;

    companion object {

        /**
         * The mode the `megaAccessibilityRunMode` instrumentation runner argument selected, or
         * [GateAndReport] when the run passed none.
         */
        val current: AccessibilityRunMode by lazy { fromRunnerArgument() }
    }
}

private const val RUN_MODE_ARGUMENT = "megaAccessibilityRunMode"

private fun fromRunnerArgument(): AccessibilityRunMode {
    val argument = InstrumentationRegistry.getArguments()
        .getString(RUN_MODE_ARGUMENT)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return AccessibilityRunMode.GateAndReport
    val mode = AccessibilityRunMode.entries
        .firstOrNull { it.name.equals(argument, ignoreCase = true) }
    // A typo would otherwise leave a gate job silently running in the default mode.
    return checkNotNull(mode) {
        "Unknown $RUN_MODE_ARGUMENT '$argument', expected one of " +
            AccessibilityRunMode.entries.joinToString { it.name }
    }
}
