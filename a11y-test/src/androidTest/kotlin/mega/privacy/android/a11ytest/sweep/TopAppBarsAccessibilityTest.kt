package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.button.SecondarySmallIconButton
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system top app bar family, covering the default
 * and search app bars with their navigation types and trailing actions.
 */
@RunWith(AndroidJUnit4::class)
class TopAppBarsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "TopAppBarsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_top_app_bar_with_back_navigation_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaTopAppBar(
                title = "Cloud drive",
                navigationType = AppBarNavigationType.Back {},
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_top_app_bar_with_subtitle_and_actions_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaTopAppBar(
                title = "Shared folder",
                navigationType = AppBarNavigationType.Close {},
                subtitle = "42 items",
                trailingIcons = {
                    SecondarySmallIconButton(
                        icon = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                        onClick = {},
                        contentDescription = "More options",
                    )
                },
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_search_top_app_bar_passes_accessibility_checks_when_the_duplicate_bounds_baseline_is_suppressed() {
        // The core-ui search app bar stacks two long-clickable elements on the search action's
        // location and cannot be fixed in this repository, so DuplicateClickableBoundsCheck is
        // baselined for this test only.
        composeTestRule.enableAccessibilityChecks(
            megaAccessibilityValidator(
                reporter = reporter,
                baselineSuppressions = matchesCheck(DuplicateClickableBoundsCheck::class.java),
            )
        )
        composeTestRule.setSweepContent {
            MegaSearchTopAppBar(
                navigationType = AppBarNavigationType.Back {},
                title = "Search",
                searchPlaceholder = "Search files",
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        val suppressedBaseline = reporter.entries.filter {
            it.checkName == "DuplicateClickableBoundsCheck" && it.resultType == "SUPPRESSED"
        }
        assertThat(suppressedBaseline).isNotEmpty()
    }

    @Test
    fun test_that_search_top_app_bar_in_searching_mode_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaSearchTopAppBar(
                navigationType = AppBarNavigationType.Back {},
                title = "Search",
                query = "holiday",
                onQueryChanged = {},
                isSearchingMode = true,
                searchPlaceholder = "Search files",
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
