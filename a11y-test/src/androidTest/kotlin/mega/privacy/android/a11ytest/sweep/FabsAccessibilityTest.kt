package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.fab.MegaFab
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system floating action button.
 */
@RunWith(AndroidJUnit4::class)
class FabsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "FabsAccessibilityTest")

    @Test
    fun test_that_fab_passes_accessibility_checks_when_the_missing_label_baseline_is_suppressed() {
        // MegaFab exposes no content description parameter and its internal icon is unlabeled,
        // so every production FAB is a clickable without speakable text. This cannot be fixed in
        // this repository, so SpeakableTextPresentCheck is baselined for this test only.
        composeTestRule.enableAccessibilityChecks(
            megaAccessibilityValidator(
                reporter = reporter,
                baselineSuppressions = matchesCheck(SpeakableTextPresentCheck::class.java),
            )
        )
        composeTestRule.setSweepContent {
            MegaFab(
                onClick = {},
                painter = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        val suppressedBaseline = reporter.entries.filter {
            it.checkName == "SpeakableTextPresentCheck" && it.resultType == "SUPPRESSED"
        }
        assertThat(suppressedBaseline).isNotEmpty()
    }
}
