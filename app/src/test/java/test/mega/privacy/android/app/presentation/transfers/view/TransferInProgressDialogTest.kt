package test.mega.privacy.android.app.presentation.transfers.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.view.CONFIRM_CANCEL_TAG
import mega.privacy.android.app.presentation.transfers.view.PROGRESS_TAG
import mega.privacy.android.app.presentation.transfers.view.TransferInProgressDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class TransferInProgressDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that progress dialog is showed when first composed`() {
        composeTestRule.setContent {
            TransferInProgressDialog(onCancelConfirmed = {})
        }
        composeTestRule.onNodeWithTag(PROGRESS_TAG).assertExists()
    }

    @Test
    fun `test that confirm dialog is not showed when first composed`() {
        composeTestRule.setContent {
            TransferInProgressDialog(onCancelConfirmed = {})
        }
        composeTestRule.onNodeWithTag(CONFIRM_CANCEL_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that confirm dialog is showed when cancel button is clicked`() {
        composeTestRule.setContent {
            TransferInProgressDialog(onCancelConfirmed = {})
        }
        composeTestRule.onNodeWithText(R.string.cancel_transfers).performClick()
        composeTestRule.onNodeWithTag(CONFIRM_CANCEL_TAG).assertExists()
    }

    @Test
    fun `test that confirm dialog is dismissed when dismiss button is clicked`() {
        composeTestRule.setContent {
            TransferInProgressDialog(onCancelConfirmed = {})
        }
        composeTestRule.onNodeWithText(R.string.cancel_transfers).performClick()
        composeTestRule.onNodeWithText(R.string.general_dismiss).performClick()
        composeTestRule.onNodeWithTag(CONFIRM_CANCEL_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that on confirm canceled is triggered when cancel is clicked`() {
        val lambdaMock = mock<() -> Unit>()
        composeTestRule.setContent {
            TransferInProgressDialog(onCancelConfirmed = lambdaMock)
        }
        composeTestRule.onNodeWithText(R.string.cancel_transfers).performClick()
        composeTestRule.onNodeWithText(R.string.button_proceed).performClick()
        verify(lambdaMock).invoke()
    }
}