package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class ClearAllTransfersDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onClearAllTransfers = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()

    @Test
    fun `test that dialog shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_CLEAR_ALL_TRANSFERS_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.option_to_clear_transfers).assertIsDisplayed()
            onNodeWithText(R.string.general_dismiss).assertIsDisplayed()
            onNodeWithText(R.string.general_clear).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on clear button calls onClearAllTransfers and onDismiss`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(CONFIRM_TAG).performClick()

        verify(onClearAllTransfers).invoke()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that clicking on dismiss button calls onDismiss but not onClearAllTransfers`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(CANCEL_TAG).performClick()

        verify(onDismiss).invoke()
        verifyNoInteractions(onClearAllTransfers)
    }

    private fun initComposeTestRule() {
        composeTestRule.setContent {
            ClearAllTransfersDialog(
                onClearAllTransfers = onClearAllTransfers,
                onDismiss = onDismiss
            )
        }
    }
}