package mega.privacy.android.core.ui.controls.lists

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuListViewItemTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that MenuListViewItem displays text when we pass text to MenuListViewItem`() {
        composeRule.setContent {
            MenuListViewItem(
                text = "MenuListViewItem",
            )
        }
        composeRule.onNodeWithText("MenuListViewItem").assertExists()
        composeRule.onNodeWithTag(testTag = TEXT_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(testTag = ICON_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = SWITCH_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that MenuListViewItem displays text,icon,switch when we pass text,icon,switch to MenuListViewItem`() {
        composeRule.setContent {
            MenuListViewItem(
                text = "MenuListViewItem",
                icon = R.drawable.ic_favorite,
                hasSwitch = true,
            )
        }
        composeRule.onNodeWithText("MenuListViewItem").assertExists()
        composeRule.onNodeWithTag(testTag = TEXT_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(testTag = ICON_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(testTag = SWITCH_TAG, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that MenuListViewItem displays text and icon when we pass text, icon resource`() {
        composeRule.setContent {
            MenuListViewItem(
                text = "MenuListViewItem",
                icon = R.drawable.ic_favorite
            )
        }
        composeRule.onNodeWithText("MenuListViewItem").assertExists()
        composeRule.onNodeWithTag(testTag = TEXT_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(testTag = ICON_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(testTag = SWITCH_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
}