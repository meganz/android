package mega.privacy.android.shared.nodes.dialog

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
internal class DiscardScanWarningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(id: Int): String = context.getString(id)

    @Test
    fun `test that the dialog and its static elements are shown`() {
        composeTestRule.setContent {
            DiscardScanWarningDialog(
                hasMultipleScans = true,
                onDiscard = {},
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithTag(DISCARD_SCAN_WARNING_DIALOG_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.scan_dialog_discard_confirmation_action))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the correct title and body are shown when only one scan will be discarded`() {
        composeTestRule.setContent {
            DiscardScanWarningDialog(
                hasMultipleScans = false,
                onDiscard = {},
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.scan_dialog_discard_confirmation_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.scan_dialog_discard_confirmation_body))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the correct title and body are shown when more than one scan will be discarded`() {
        composeTestRule.setContent {
            DiscardScanWarningDialog(
                hasMultipleScans = true,
                onDiscard = {},
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.scan_dialog_discard_all_confirmation_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.scan_dialog_discard_all_confirmation_body))
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking the discard button invokes onDiscard`() {
        val onDiscard = mock<() -> Unit>()
        composeTestRule.setContent {
            DiscardScanWarningDialog(
                hasMultipleScans = true,
                onDiscard = onDiscard,
                onCancel = {},
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.scan_dialog_discard_confirmation_action))
            .performClick()

        verify(onDiscard).invoke()
    }

    @Test
    fun `test that clicking the cancel button invokes onCancel`() {
        val onCancel = mock<() -> Unit>()
        composeTestRule.setContent {
            DiscardScanWarningDialog(
                hasMultipleScans = true,
                onDiscard = {},
                onCancel = onCancel,
            )
        }

        composeTestRule.onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .performClick()

        verify(onCancel).invoke()
    }
}
