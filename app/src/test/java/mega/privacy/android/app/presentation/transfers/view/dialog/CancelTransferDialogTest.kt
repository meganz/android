package mega.privacy.android.app.presentation.transfers.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CONFIRM_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class CancelTransferDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val title = "Title"
    private val onCancelTransfer = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()

    @Test
    fun `test that dialog shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_CANCEL_TRANSFER_DIALOG).assertIsDisplayed()
            onNodeWithText(title).assertIsDisplayed()
            onNodeWithText(R.string.general_dismiss).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on cancel button calls onCancelTransfer but not onDismiss`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(CONFIRM_TAG).performClick()

        verify(onCancelTransfer).invoke()
        verifyNoInteractions(onDismiss)
    }

    @Test
    fun `test that clicking on dismiss button calls onDismiss but not onCancelTransfer`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(CANCEL_TAG).performClick()

        verify(onDismiss).invoke()
        verifyNoInteractions(onCancelTransfer)
    }

    private fun initComposeTestRule() {
        composeTestRule.setContent {
            CancelTransferDialog(
                title = title,
                onCancelTransfer = onCancelTransfer,
                onDismiss = onDismiss
            )
        }
    }
}