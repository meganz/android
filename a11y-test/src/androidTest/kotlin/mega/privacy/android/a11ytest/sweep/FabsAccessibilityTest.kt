package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.fab.MegaFab
import mega.privacy.android.a11ytest.ThemeReporters
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

    private val reporters = ThemeReporters("FabsAccessibilityTest")

    @Test
    fun test_that_fab_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            MegaFab(
                onClick = {},
                painter = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                contentDescription = "Close",
            )
        }

        sweep.checkInEachTheme()
    }
}
