package mega.privacy.android.app.presentation.transfers.view.dialog

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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class CancelAllTransfersDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onCancelAllTransfers = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()

    @Test
    fun `test that dialog shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_CANCEL_ALL_TRANSFERS_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.cancel_all_transfer_confirmation).assertIsDisplayed()
            onNodeWithText(R.string.general_dismiss).assertIsDisplayed()
            onNodeWithText(R.string.cancel_all_action).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on cancel all transfers button calls onCancelAllTransfers and onDismiss`() {
        initComposeTestRule()

        composeTestRule.onNodeWithText(R.string.cancel_all_action).performClick()

        verify(onCancelAllTransfers).invoke()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that clicking on dismiss button calls onDismiss but not onCancelAllTransfers`() {
        initComposeTestRule()

        composeTestRule.onNodeWithText(R.string.general_dismiss).performClick()

        verify(onDismiss).invoke()
        verifyNoInteractions(onCancelAllTransfers)
    }

    private fun initComposeTestRule() {
        composeTestRule.setContent {
            CancelAllTransfersDialog(
                onCancelAllTransfers = onCancelAllTransfers,
                onDismiss = onDismiss
            )
        }
    }
}