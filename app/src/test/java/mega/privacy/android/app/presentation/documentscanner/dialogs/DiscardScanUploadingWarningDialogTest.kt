package mega.privacy.android.app.presentation.documentscanner.dialogs

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

/**
 * Test class for [DiscardScanUploadingWarningDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class DiscardScanUploadingWarningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog and its static elements are shown`() {
        composeTestRule.setContent {
            DiscardScanUploadingWarningDialog(
                hasMultipleScans = true,
                onWarningAcknowledged = {},
                onWarningDismissed = {}
            )
        }

        composeTestRule.onNodeWithTag(EXIT_SAVE_SCANNED_DOCUMENTS_SCREEN_WARNING_DIALOG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.scan_dialog_discard_action)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(SharedR.string.general_dialog_cancel_button)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the correct title and body are shown when only one scan will be discarded`() {
        composeTestRule.setContent {
            DiscardScanUploadingWarningDialog(
                hasMultipleScans = false,
                onWarningAcknowledged = {},
                onWarningDismissed = {}
            )
        }

        composeTestRule.onNodeWithText(R.string.scan_dialog_discard_title)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.scan_dialog_discard_body)
            .assertIsDisplayed()
    }

    @Test
    fun `test that the correct title and body are shown when more than one scan will be discarded`() {
        composeTestRule.setContent {
            DiscardScanUploadingWarningDialog(
                hasMultipleScans = true,
                onWarningAcknowledged = {},
                onWarningDismissed = {}
            )
        }

        composeTestRule.onNodeWithText(R.string.scan_dialog_discard_all_title)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.scan_dialog_discard_all_body)
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking the confirmation button invokes the correct lambda`() {
        val onWarningAcknowledged = mock<() -> Unit>()
        composeTestRule.setContent {
            DiscardScanUploadingWarningDialog(
                hasMultipleScans = true,
                onWarningAcknowledged = onWarningAcknowledged,
                onWarningDismissed = {}
            )
        }

        composeTestRule.onNodeWithText(R.string.scan_dialog_discard_action)
            .performClick()

        verify(onWarningAcknowledged).invoke()
    }

    @Test
    fun `test that clicking the cancel button invokes the correct lambda`() {
        val onWarningDismissed = mock<() -> Unit>()
        composeTestRule.setContent {
            DiscardScanUploadingWarningDialog(
                hasMultipleScans = true,
                onWarningAcknowledged = {},
                onWarningDismissed = onWarningDismissed,
            )
        }

        composeTestRule.onNodeWithText(SharedR.string.general_dialog_cancel_button)
            .performClick()

        verify(onWarningDismissed).invoke()
    }
}