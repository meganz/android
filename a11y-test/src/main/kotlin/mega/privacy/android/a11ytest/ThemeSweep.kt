package mega.privacy.android.a11ytest

import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import org.hamcrest.Matcher

/**
 * The themes an accessibility sweep can be run in; which of them a given run covers is
 * [AccessibilityRunMode.themes].
 *
 * Contrast is a function of the pixels the checks measure, so a light-only sweep is blind to
 * every dark-theme text and image contrast finding.
 *
 * @property isDark whether the design system renders its dark palette in this theme
 */
enum class SweepTheme(val isDark: Boolean) {

    /** The light palette. */
    Light(isDark = false),

    /** The dark palette. */
    Dark(isDark = true),
}

/**
 * One [AccessibilityCheckReporter] per [SweepTheme], so the JSON reports a run leaves behind
 * name the theme each finding was measured in and design can act on them without guessing.
 *
 * A reporter is held for every theme whether or not [mode] sweeps it, so a test can ask for a
 * theme's findings without first asking whether this run measured them.
 *
 * @param reportName prefix for the generated report file names, typically the test class name
 * @property mode the accessibility policy this run applies
 */
class ThemeReporters(
    reportName: String,
    val mode: AccessibilityRunMode = AccessibilityRunMode.current,
) {

    private val reporters = SweepTheme.entries.associateWith { theme ->
        AccessibilityCheckReporter(
            reportName = "$reportName-${theme.name.lowercase()}",
            mode = mode,
        )
    }

    /**
     * Every check result recorded so far, in either theme.
     */
    val entries: List<AccessibilityCheckReportEntry>
        get() = reporters.values.flatMap { it.entries }

    /**
     * The check results recorded so far in [theme].
     */
    fun entriesIn(theme: SweepTheme): List<AccessibilityCheckReportEntry> =
        reporterFor(theme).entries

    /**
     * The reporter collecting [theme]'s results.
     */
    fun reporterFor(theme: SweepTheme): AccessibilityCheckReporter = reporters.getValue(theme)
}

/**
 * A mounted composition whose theme can be switched in place, returned by the mounting helpers.
 *
 * A Compose test rule accepts `setContent` only once, so covering a second theme by mounting
 * again would mean a second test method and a second activity launch for every sweep. Reading
 * the theme flag from a snapshot state instead lets one mount be recomposed into the other
 * theme and checked again, which costs a recomposition rather than a test.
 */
class ThemedContent internal constructor(
    private val composeTestRule: ComposeContentTestRule,
    private val darkTheme: MutableState<Boolean>,
    private val reporters: ThemeReporters,
    private val baselineSuppressions: Matcher<AccessibilityCheckResult>?,
) {

    /**
     * Runs [check] against the mounted content once per theme the run's
     * [AccessibilityRunMode.themes] covers, recomposing into each theme first and recording that
     * theme's results into its own reporter. A gate run covers light theme alone, so it checks
     * the mount once.
     *
     * @param check triggers the accessibility checks; defaults to checking the whole root, and
     * is overridden by sweeps whose content renders in its own window.
     */
    fun checkInEachTheme(
        check: ComposeContentTestRule.() -> Unit = {
            onRoot().tryPerformAccessibilityChecks()
        },
    ) {
        reporters.mode.themes.forEach { theme ->
            recomposeInto(theme)
            composeTestRule.enableAccessibilityChecks(
                megaAccessibilityValidator(
                    reporter = reporters.reporterFor(theme),
                    baselineSuppressions = baselineSuppressions,
                )
            )
            composeTestRule.check()
        }
    }

    private fun recomposeInto(theme: SweepTheme) {
        if (darkTheme.value == theme.isDark) return
        composeTestRule.runOnUiThread { darkTheme.value = theme.isDark }
        // Sweeps that freeze an endless animation pause the clock, and a paused clock produces
        // no frame — and therefore no recomposition — until one is asked for explicitly.
        if (!composeTestRule.mainClock.autoAdvance) {
            composeTestRule.mainClock.advanceTimeByFrame()
        }
        composeTestRule.waitForIdle()
    }
}
