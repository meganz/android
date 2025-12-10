package mega.privacy.mobile.home.presentation.recents.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentsOptionsBottomSheetTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that hide menu item is displayed when isHideRecentsEnabled is false`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsOptionsBottomSheetContent(
                    isHideRecentsEnabled = false,
                    onShowRecentActivity = {},
                    onHideRecentActivity = {}
                )
            }
        }

        composeRule.onNodeWithTag(HIDE_RECENT_MENU_ITEM_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that show menu item is displayed when isHideRecentsEnabled is true`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsOptionsBottomSheetContent(
                    isHideRecentsEnabled = true,
                    onShowRecentActivity = {},
                    onHideRecentActivity = {}
                )
            }
        }

        composeRule.onNodeWithTag(SHOW_RECENT_MENU_ITEM_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onHideRecentActivity is called when hide menu item is clicked`() {
        var hideClicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsOptionsBottomSheetContent(
                    isHideRecentsEnabled = false,
                    onShowRecentActivity = {},
                    onHideRecentActivity = { hideClicked = true }
                )
            }
        }

        composeRule.onNodeWithTag(HIDE_RECENT_MENU_ITEM_TEST_TAG, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
        assertThat(hideClicked).isTrue()
    }

    @Test
    fun `test that onShowRecentActivity is called when show menu item is clicked`() {
        var showClicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsOptionsBottomSheetContent(
                    isHideRecentsEnabled = true,
                    onShowRecentActivity = { showClicked = true },
                    onHideRecentActivity = {}
                )
            }
        }

        composeRule.onNodeWithTag(SHOW_RECENT_MENU_ITEM_TEST_TAG, useUnmergedTree = true)
            .performClick()

        composeRule.waitForIdle()
        assertThat(showClicked).isTrue()
    }
}

