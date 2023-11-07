package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.legacy.core.ui.controls.controlssliders.MegaSwitch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuActionListTileTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that MenuListViewItem displays text when we pass text to MenuListViewItem`() {
        composeRule.setContent {
            MenuActionListTile(
                text = "MenuListViewItem",
            )
        }
        composeRule.onNodeWithText("MenuListViewItem").assertExists()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_TEXT_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_ICON_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_SWITCH_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that MenuListViewItem displays text,icon,switch when we pass text,icon,switch to MenuListViewItem`() {
        val switchTag = "Mega Switch"
        composeRule.setContent {
            MenuActionListTile(
                text = "MenuListViewItem",
                icon = painterResource(id = R.drawable.ic_favorite),
            ) {
                MegaSwitch(
                    modifier = Modifier.testTag(switchTag),
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }
        composeRule.onNodeWithText("MenuListViewItem").assertExists()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_TEXT_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_ICON_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(testTag = switchTag, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `test that MenuListViewItem displays text and icon when we pass text, icon resource`() {
        composeRule.setContent {
            MenuActionListTile(
                text = "MenuListViewItem",
                icon = painterResource(id = R.drawable.ic_favorite)
            )
        }
        composeRule.onNodeWithText("MenuListViewItem").assertExists()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_TEXT_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_ICON_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(testTag = MENU_ITEM_SWITCH_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }
}