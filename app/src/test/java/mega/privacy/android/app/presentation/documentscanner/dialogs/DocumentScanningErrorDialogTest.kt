package mega.privacy.android.app.presentation.documentscanner.dialogs

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.documentscanner.model.DocumentScanningError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [DocumentScanningErrorDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class DocumentScanningErrorDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog and its static elements are not shown when the error is null`() {
        composeTestRule.setContent {
            DocumentScanningErrorDialog(
                documentScanningError = null,
                onErrorAcknowledged = {},
                onErrorDismissed = {},
            )
        }

        composeTestRule.onNodeWithTag(DOCUMENT_SCANNING_ERROR_DIALOG).assertDoesNotExist()
        composeTestRule.onNodeWithText(SharedR.string.document_scanning_error_dialog_title)
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(SharedR.string.document_scanning_error_dialog_confirm_button)
            .assertDoesNotExist()
    }

    @Test
    fun `test that the dialog and its static elements are shown when an error is provided`() {
        composeTestRule.setContent {
            DocumentScanningErrorDialog(
                documentScanningError = DocumentScanningError.GenericError,
                onErrorAcknowledged = {},
                onErrorDismissed = {},
            )
        }

        composeTestRule.onNodeWithTag(DOCUMENT_SCANNING_ERROR_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(SharedR.string.document_scanning_error_dialog_title)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(SharedR.string.document_scanning_error_dialog_confirm_button)
            .assertIsDisplayed()
    }

    @Test
    fun `test that an insufficient RAM error message is shown`() {
        composeTestRule.setContent {
            DocumentScanningErrorDialog(
                documentScanningError = DocumentScanningError.InsufficientRAM,
                onErrorAcknowledged = {},
                onErrorDismissed = {},
            )
        }

        composeTestRule.onNodeWithText(SharedR.string.document_scanning_error_type_insufficient_ram)
            .assertIsDisplayed()
    }

    @Test
    fun `test that a generic error message is shown`() {
        composeTestRule.setContent {
            DocumentScanningErrorDialog(
                documentScanningError = DocumentScanningError.GenericError,
                onErrorAcknowledged = {},
                onErrorDismissed = {},
            )
        }
    }

    @Test
    fun `test that clicking the button invokes the on error acknowledged lambda`() {
        val onErrorAcknowledged = mock<() -> Unit>()
        composeTestRule.setContent {
            DocumentScanningErrorDialog(
                documentScanningError = DocumentScanningError.GenericError,
                onErrorAcknowledged = onErrorAcknowledged,
                onErrorDismissed = {},
            )
        }

        composeTestRule.onNodeWithText(SharedR.string.document_scanning_error_dialog_confirm_button)
            .performClick()

        verify(onErrorAcknowledged).invoke()
    }
}