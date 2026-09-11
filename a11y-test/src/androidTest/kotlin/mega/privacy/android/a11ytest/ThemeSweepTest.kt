package mega.privacy.android.a11ytest

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.a11ytest.sweep.setSweepContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves that switching a mounted sweep into dark theme really re-renders it and re-measures the
 * checks against the new pixels, rather than replaying the light-theme results.
 */
@RunWith(AndroidJUnit4::class)
class ThemeSweepTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Pinned rather than left to the run's mode: this test asserts what a run covering both
    // themes measures, so it has to keep asserting it in a light-only gate run.
    private val reporters = ThemeReporters("ThemeSweepTest", AccessibilityRunMode.GateAndReport)

    @Test
    fun test_that_checkInEachTheme_records_the_contrast_violation_belonging_to_each_theme() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            BasicText(text = "Near black text", style = TextStyle(color = NEAR_BLACK))
            BasicText(text = "Light grey text", style = TextStyle(color = LIGHT_GREY))
        }

        sweep.checkInEachTheme()

        // Each colour is illegible on exactly one of the two page backgrounds, so a pass that
        // reports the other theme's violation — or repeats the same measured ratio — did not
        // re-render before re-checking.
        val lightRatios = reporters.failingContrastRatiosIn(SweepTheme.Light)
        val darkRatios = reporters.failingContrastRatiosIn(SweepTheme.Dark)
        assertThat(lightRatios).isNotEmpty()
        assertThat(darkRatios).isNotEmpty()
        assertThat(darkRatios).containsNoneIn(lightRatios)
    }

    @Test
    fun test_that_checkInEachTheme_skips_the_dark_pass_in_gate_mode() {
        val gateReporters = ThemeReporters("ThemeSweepTest-gate", AccessibilityRunMode.Gate)
        // A gate-category finding, so gate mode records it, baselined so it does not fail this
        // test. It is theme-independent, so a dark pass would record it a second time.
        val sweep = composeTestRule.setSweepContent(
            reporters = gateReporters,
            baselineSuppressions = matchesCheck(SpeakableTextPresentCheck::class.java),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Blue)
                    .clickable {},
            )
        }

        sweep.checkInEachTheme()

        assertThat(gateReporters.entriesIn(SweepTheme.Light).map { it.checkName })
            .contains(SpeakableTextPresentCheck::class.java.simpleName)
        assertThat(gateReporters.entriesIn(SweepTheme.Dark)).isEmpty()
    }
}

private fun ThemeReporters.failingContrastRatiosIn(theme: SweepTheme): List<Double> =
    entriesIn(theme)
        .filter { it.checkName == "TextContrastCheck" }
        .mapNotNull { it.metadata[TextContrastCheck.KEY_CONTRAST_RATIO] as? Double }
        .filter { it < MINIMUM_SMALL_TEXT_CONTRAST_RATIO }

private val NEAR_BLACK = Color(0xFF121212)

private val LIGHT_GREY = Color(0xFFC8C8C8)

private const val MINIMUM_SMALL_TEXT_CONTRAST_RATIO = 4.5
