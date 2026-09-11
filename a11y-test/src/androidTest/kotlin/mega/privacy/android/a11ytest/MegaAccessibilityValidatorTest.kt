package mega.privacy.android.a11ytest

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.AccessibilityViewCheckException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MegaAccessibilityValidatorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Pinned rather than left to the run's mode: these tests assert the policy of a specific
    // mode, so they have to keep asserting it in a run that selected another one.
    private val reporter = reporterFor(AccessibilityRunMode.GateAndReport)

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_accessibility_checks_throw_when_a_clickable_element_has_no_label() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Blue)
                        .clickable {},
                )
            }
        }

        assertThrows(AccessibilityViewCheckException::class.java) {
            composeTestRule.onRoot().tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun test_that_accessibility_checks_throw_when_a_touch_target_is_undersized() {
        composeTestRule.setContent {
            // Compose expands the accessibility touch bounds of small clickables to the platform
            // minimum, which would legitimately satisfy the check; disabling the expansion
            // reproduces a component that opts out of it.
            val viewConfiguration = object : ViewConfiguration by LocalViewConfiguration.current {
                override val minimumTouchTargetSize: DpSize get() = DpSize.Zero
            }
            CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
                Box(
                    modifier = Modifier
                        .size(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.Blue)
                            .clickable {}
                            .semantics { contentDescription = "Undersized target" },
                    )
                }
            }
        }

        assertThrows(AccessibilityViewCheckException::class.java) {
            composeTestRule.onRoot().tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun test_that_accessibility_checks_do_not_throw_but_report_when_text_contrast_is_low() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color(0xFFFAFBFC)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Low contrast text",
                    style = TextStyle(color = Color(0xFFB0B1B2)),
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Suppression rewrites every TextContrastCheck result to SUPPRESSED, so a real finding
        // is distinguished by its measured contrast ratio.
        val contrastFindings = reporter.entries.filter {
            it.checkName == "TextContrastCheck" &&
                it.resultType == "SUPPRESSED" &&
                it.metadata.containsKey(TextContrastCheck.KEY_CONTRAST_RATIO)
        }
        assertThat(contrastFindings).isNotEmpty()
    }

    @Test
    fun test_that_accessibility_checks_pass_when_content_is_compliant() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Blue)
                        .clickable {}
                        .semantics { contentDescription = "Compliant target" },
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        val gateFindings = reporter.entries.filter { it.resultType == "ERROR" }
        assertThat(gateFindings).isEmpty()
    }

    @Test
    fun test_that_gate_mode_records_no_design_findings_when_text_contrast_is_low() {
        val gateReporter = enableChecksIn(AccessibilityRunMode.Gate)
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color(0xFFFAFBFC)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Low contrast text",
                    style = TextStyle(color = Color(0xFFB0B1B2)),
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        assertThat(gateReporter.entries.map { it.checkName })
            .containsNoneIn(designOwnedCheckNames)
    }

    @Test
    fun test_that_report_mode_records_but_does_not_throw_when_a_clickable_element_has_no_label() {
        val reportReporter = enableChecksIn(AccessibilityRunMode.Report)
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Blue)
                        .clickable {},
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        val unlabelledFindings = reportReporter.entries.filter {
            it.checkName == SpeakableTextPresentCheck::class.java.simpleName &&
                it.resultType == "ERROR"
        }
        assertThat(unlabelledFindings).isNotEmpty()
    }

    private fun enableChecksIn(mode: AccessibilityRunMode): AccessibilityCheckReporter =
        reporterFor(mode).also {
            composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(it))
        }
}

private fun reporterFor(mode: AccessibilityRunMode) = AccessibilityCheckReporter(
    reportName = "MegaAccessibilityValidatorTest-${mode.name}",
    mode = mode,
)
