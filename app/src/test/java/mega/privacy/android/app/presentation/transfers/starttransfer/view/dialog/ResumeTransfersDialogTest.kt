package mega.privacy.android.app.presentation.transfers.starttransfer.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.fromId
import mega.privacy.android.app.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ResumeTransfersDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val onResume = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()

    @Test
    fun `test that resume chat transfers dialog is correctly displayed`() {
        initChatComposeRule()
        composeRule.apply {
            onNodeWithText(R.string.warning_resume_transfers).assertIsDisplayed()
            onNodeWithText(R.string.warning_message_resume_transfers).assertIsDisplayed()
            onNodeWithText(R.string.button_resume_individual_transfer).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that resume preview transfers dialog is correctly displayed`() {
        initPreviewComposeRule()
        composeRule.apply {
            onNodeWithText(sharedR.string.transfers_preview_paused_dialog_title).assertIsDisplayed()
            onNodeWithText(
                fromId(
                    sharedR.string.transfers_preview_paused_dialog_text,
                    fileName
                )
            ).assertIsDisplayed()
            onNodeWithText(R.string.button_resume_individual_transfer).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that onResume is invoked when confirm button is clicked`() {
        initChatComposeRule()
        composeRule.onNodeWithText(R.string.button_resume_individual_transfer).performClick()
        verify(onResume).invoke()
    }

    @Test
    fun `test that onDismiss is invoked when dismiss button is clicked`() {
        initChatComposeRule()
        composeRule.onNodeWithText(sharedR.string.general_dialog_cancel_button).performClick()
        verify(onDismiss).invoke()
    }

    private fun initChatComposeRule() {
        composeRule.setContent {
            ResumeChatTransfersDialog(onResume = onResume, onDismiss = onDismiss)
        }
    }

    private fun initPreviewComposeRule() {
        composeRule.setContent {
            ResumePreviewTransfersDialog(
                fileName = fileName,
                onResume = onResume,
                onDismiss = onDismiss
            )
        }
    }
}

private const val fileName = "fileName"