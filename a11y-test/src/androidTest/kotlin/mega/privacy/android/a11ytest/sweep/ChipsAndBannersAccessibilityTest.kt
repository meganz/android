package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.banner.InlineWarningBanner
import mega.android.core.ui.components.banner.TopWarningBanner
import mega.android.core.ui.components.chip.MegaChip
import mega.privacy.android.a11ytest.ThemeReporters
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

    private val reporters = ThemeReporters("ChipsAndBannersAccessibilityTest")

    @Test
    fun test_that_chips_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
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

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_inline_banners_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
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

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_top_banner_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            TopWarningBanner(
                body = "You are offline. Changes will sync later.",
                showCancelButton = false,
                title = "No connection",
            )
        }

        sweep.checkInEachTheme()
    }
}
