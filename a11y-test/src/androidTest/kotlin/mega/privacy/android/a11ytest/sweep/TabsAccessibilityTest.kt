package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.tabs.MegaFixedTabRow
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.model.TabItems
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system tab row family, covering the fixed and
 * scrollable tab rows with selected, badged and overflowing tabs.
 */
@RunWith(AndroidJUnit4::class)
class TabsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "TabsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_fixed_tab_row_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaFixedTabRow(
                tabIndex = 0,
                items = listOf(
                    TabItems(title = "Chats"),
                    TabItems(title = "Meetings", showBadge = true),
                    TabItems(title = "Calls"),
                ),
                onClick = {},
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_scrollable_tab_row_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaScrollableTabRow(
                tabIndex = 1,
                items = listOf(
                    TabItems(title = "Recents"),
                    TabItems(title = "Favourites"),
                    TabItems(title = "Offline", showBadge = true),
                    TabItems(title = "Shared with me"),
                    TabItems(title = "Links"),
                    TabItems(title = "Rubbish bin"),
                ),
                onClick = {},
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
