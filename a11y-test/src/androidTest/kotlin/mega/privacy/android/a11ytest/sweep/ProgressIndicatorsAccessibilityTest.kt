package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.components.indicators.LargeSpinnerIndicator
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.components.indicators.ProgressBarIndicator
import mega.android.core.ui.components.indicators.SmallInfiniteSpinnerIndicator
import mega.android.core.ui.components.indicators.SmallSpinnerIndicator
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system progress indicator family, covering the
 * determinate and indeterminate linear bars and spinners.
 */
@RunWith(AndroidJUnit4::class)
class ProgressIndicatorsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter =
        AccessibilityCheckReporter(reportName = "ProgressIndicatorsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_determinate_progress_indicators_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
            ProgressBarIndicator(progressPercentage = 0f)
            ProgressBarIndicator(progressPercentage = 50f)
            ProgressBarIndicator(progressPercentage = 100f)
            SmallSpinnerIndicator(progressPercentage = 25f)
            LargeSpinnerIndicator(progressPercentage = 75f)
            MegaAnimatedLinearProgressIndicator(indicatorProgress = 0.5f)
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_indeterminate_progress_indicators_pass_accessibility_checks() {
        // Indeterminate indicators animate forever, so Compose never reports idle while the
        // clock auto-advances; pausing it renders a single stable frame to check against.
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setSweepContent {
            InfiniteProgressBarIndicator()
            SmallInfiniteSpinnerIndicator()
            LargeInfiniteSpinnerIndicator()
        }
        composeTestRule.mainClock.advanceTimeBy(500)

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
