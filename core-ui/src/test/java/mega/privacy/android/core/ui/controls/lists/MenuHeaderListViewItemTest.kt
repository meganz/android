package mega.privacy.android.core.ui.controls.lists

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuHeaderListViewItemTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that MenuHeaderListViewItem displays text when we pass text to MenuHeaderListViewItem`() {
        composeRule.setContent {
            MenuHeaderListViewItem(
                text = "MenuHeaderListViewItem",
            )
        }
        composeRule.onNodeWithText("MenuHeaderListViewItem").assertExists()
        composeRule.onNodeWithTag(testTag = MENU_HEADER_TEXT_TAG, useUnmergedTree = true)
            .assertExists()
    }
}