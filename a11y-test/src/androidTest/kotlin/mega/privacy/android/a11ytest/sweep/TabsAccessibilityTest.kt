package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.tabs.MegaFixedTabRow
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.model.TabItems
import mega.privacy.android.a11ytest.ThemeReporters
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

    private val reporters = ThemeReporters("TabsAccessibilityTest")

    @Test
    fun test_that_fixed_tab_row_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
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

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_scrollable_tab_row_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
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

        sweep.checkInEachTheme()
    }
}
