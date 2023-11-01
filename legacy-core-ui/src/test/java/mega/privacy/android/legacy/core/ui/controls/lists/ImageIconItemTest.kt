package mega.privacy.android.legacy.core.ui.controls.lists

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageIconItemTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that ImageIconItem displays image when we pass image`() {
        composeRule.setContent {
            ImageIconItem(
                icon = R.drawable.ic_recovery_key_circle,
                title = R.string.back_up_recovery_key,
                description = "Test description",
                isIconMode = false,
                testTag = "ADD_PHONE_NUMBER",
            )
        }
        composeRule.onNodeWithTag(testTag = IMAGE_ITEM_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(testTag = ICON_ITEM_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that ImageIconItem displays icon when we pass icon`() {
        composeRule.setContent {
            ImageIconItem(
                icon = R.drawable.ic_contacts_connection,
                title = R.string.back_up_recovery_key,
                description = "Test description",
                isIconMode = true,
                testTag = "ADD_PHONE_NUMBER",
            )
        }
        composeRule.onNodeWithTag(testTag = ICON_ITEM_TAG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(testTag = IMAGE_ITEM_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that ImageIconItem displays title and description correctly`() {
        composeRule.setContent {
            ImageIconItem(
                icon = R.drawable.ic_contacts_connection,
                title = R.string.back_up_recovery_key,
                description = "Test description",
                isIconMode = true,
                testTag = "ADD_PHONE_NUMBER",
            )
        }
        composeRule.onNodeWithText("Back up Recovery key").assertExists()
        composeRule.onNodeWithText("Test description").assertExists()
    }

    @Test
    fun `test that ImageIconItem displays divider when we pass true to withDivider parameter`() {
        composeRule.setContent {
            ImageIconItem(
                icon = R.drawable.ic_recovery_key_circle,
                title = R.string.back_up_recovery_key,
                description = "Test description",
                isIconMode = false,
                testTag = "ADD_PHONE_NUMBER",
                withDivider = true,
            )
        }
        composeRule.onNodeWithTag(testTag = IMAGE_ICON_ITEM_DIVIDER_TAG, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `test that ImageIconItem does not display divider when we don't pass withDivider parameter`() {
        composeRule.setContent {
            ImageIconItem(
                icon = R.drawable.ic_recovery_key_circle,
                title = R.string.back_up_recovery_key,
                description = "Test description",
                isIconMode = false,
                testTag = "ADD_PHONE_NUMBER",
            )
        }
        composeRule.onNodeWithTag(testTag = IMAGE_ICON_ITEM_DIVIDER_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that ImageIconItem assign test tag to layout when we pass testTag parameter`() {
        composeRule.setContent {
            ImageIconItem(
                icon = R.drawable.ic_recovery_key_circle,
                title = R.string.back_up_recovery_key,
                description = "Test description",
                isIconMode = false,
                testTag = "ADD_PHONE_NUMBER",
            )
        }
        composeRule.onNodeWithTag(testTag = "ADD_PHONE_NUMBER").assertExists()
    }
}