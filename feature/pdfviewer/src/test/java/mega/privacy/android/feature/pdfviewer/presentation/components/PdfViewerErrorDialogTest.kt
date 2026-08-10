package mega.privacy.android.feature.pdfviewer.presentation.components

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class PdfViewerErrorDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that dialog is displayed with correct test tag`() {
        initComposeRuleContent(error = PdfViewerError.FileNotFound)

        composeRule.onNodeWithTag(PDF_VIEWER_ERROR_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that LoadError online shows corrupt or missing message`() {
        initComposeRuleContent(error = PdfViewerError.LoadError(null), isOnline = true)

        val message = composeRule.activity.getString(sharedR.string.pdf_viewer_error_corrupt_or_missing)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that LoadError offline shows no network message`() {
        initComposeRuleContent(error = PdfViewerError.LoadError(null), isOnline = false)

        val message = composeRule.activity.getString(sharedR.string.pdf_viewer_error_no_network)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that NetworkError shows no network message`() {
        initComposeRuleContent(error = PdfViewerError.NetworkError)

        val message = composeRule.activity.getString(sharedR.string.pdf_viewer_error_no_network)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that FileNotFound shows file not found message`() {
        initComposeRuleContent(error = PdfViewerError.FileNotFound)

        val message = composeRule.activity.getString(sharedR.string.general_error_file_not_found)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that StreamingError shows streaming failed message`() {
        initComposeRuleContent(error = PdfViewerError.StreamingError(null))

        val message = composeRule.activity.getString(sharedR.string.pdf_viewer_error_streaming)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that PasswordProtected shows incorrect password message`() {
        initComposeRuleContent(error = PdfViewerError.PasswordProtected)

        val message = composeRule.activity.getString(sharedR.string.pdf_viewer_dialog_error_incorrect_password)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that InvalidPassword shows incorrect password message`() {
        initComposeRuleContent(error = PdfViewerError.InvalidPassword)

        val message = composeRule.activity.getString(sharedR.string.pdf_viewer_dialog_error_incorrect_password)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that Generic error shows generic failed message`() {
        initComposeRuleContent(error = PdfViewerError.Generic(Throwable("error")))

        val message = composeRule.activity.getString(sharedR.string.general_request_failed_message)
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun `test that clicking ok button invokes onDismiss`() {
        val onDismiss = mock<() -> Unit>()
        initComposeRuleContent(error = PdfViewerError.FileNotFound, onDismiss = onDismiss)

        val okLabel = composeRule.activity.getString(sharedR.string.general_ok_only)
        composeRule.onNodeWithText(okLabel).performClick()

        verify(onDismiss).invoke()
    }

    private fun initComposeRuleContent(
        error: PdfViewerError,
        isOnline: Boolean = true,
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            PdfViewerErrorDialog(
                error = error,
                isOnline = isOnline,
                onDismiss = onDismiss,
            )
        }
    }
}
