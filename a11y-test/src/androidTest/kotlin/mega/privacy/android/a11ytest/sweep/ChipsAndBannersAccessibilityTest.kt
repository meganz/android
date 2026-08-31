package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.banner.InlineWarningBanner
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.chip.MegaChip
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system chip and banner families, covering chip
 * selection states and the inline and top banner variants.
 */
@RunWith(AndroidJUnit4::class)
class ChipsAndBannersAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter =
        AccessibilityCheckReporter(reportName = "ChipsAndBannersAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_chips_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaChip(
                selected = true,
                text = "Images",
                onClick = {},
            )
            MegaChip(
                selected = false,
                text = "Videos",
                onClick = {},
            )
            MegaChip(
                selected = false,
                text = "Documents",
                enabled = false,
                onClick = {},
            )
            MegaChip(
                selected = true,
                text = "Audio",
                leadingIcon = CoreUiR.drawable.ic_close_medium_thin_outline,
                onClick = {},
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_inline_banners_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
            InlineWarningBanner(
                body = "Your storage is almost full.",
                showCancelButton = true,
                title = "Storage warning",
                actionButtonText = "Upgrade now",
            )
            InlineErrorBanner(
                body = "Something went wrong while syncing.",
                showCancelButton = true,
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_top_banner_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            TopWarningBanner(
                body = "You are offline. Changes will sync later.",
                showCancelButton = false,
                title = "No connection",
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
