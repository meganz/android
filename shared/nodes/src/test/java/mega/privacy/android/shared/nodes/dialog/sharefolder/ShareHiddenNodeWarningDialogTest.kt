package mega.privacy.android.shared.nodes.dialog.sharefolder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
internal class ShareHiddenNodeWarningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(id: Int): String = context.getString(id)

    @Test
    fun `test that the dialog and its buttons are shown`() {
        composeTestRule.setContent {
            ShareHiddenNodeWarningDialog(
                sharingMultipleFolders = false,
                onConfirm = {},
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithTag(SHARE_HIDDEN_NODE_WARNING_DIALOG_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.button_continue)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the single-folder title and body are shown when sharing one folder`() {
        composeTestRule.setContent {
            ShareHiddenNodeWarningDialog(
                sharingMultipleFolders = false,
                onConfirm = {},
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.hidden_item)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.share_hidden_folder_description))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the multiple-folders title and body are shown when sharing several folders`() {
        composeTestRule.setContent {
            ShareHiddenNodeWarningDialog(
                sharingMultipleFolders = true,
                onConfirm = {},
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.hidden_items)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.share_hidden_folders_description))
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking continue invokes onConfirm`() {
        val onConfirm = mock<() -> Unit>()
        composeTestRule.setContent {
            ShareHiddenNodeWarningDialog(
                sharingMultipleFolders = false,
                onConfirm = onConfirm,
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.button_continue)).performClick()

        verify(onConfirm).invoke()
    }

    @Test
    fun `test that clicking cancel invokes onCancel`() {
        val onCancel = mock<() -> Unit>()
        composeTestRule.setContent {
            ShareHiddenNodeWarningDialog(
                sharingMultipleFolders = false,
                onConfirm = {},
                onCancel = onCancel,
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .performClick()

        verify(onCancel).invoke()
    }
}
