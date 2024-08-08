package test.mega.privacy.android.app.presentation.transfers.view.sheet

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.view.sheet.InProgressActionsBottomSheet
import mega.privacy.android.app.presentation.transfers.view.sheet.TEST_TAG_CANCEL_ALL_ACTION
import mega.privacy.android.app.presentation.transfers.view.sheet.TEST_TAG_IN_PROGRESS_ACTIONS_PANEL
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class InProgressActionsBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onCancelAllTransfers = mock<() -> Unit>()

    @Test
    fun `test that sheet shows cancel all transfers option`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_IN_PROGRESS_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CANCEL_ALL_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.menu_cancel_all_transfers).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on cancel all transfers option invokes onCancelAllTransfers`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_CANCEL_ALL_ACTION).performClick()

        verify(onCancelAllTransfers).invoke()
    }

    private fun initComposeTestRule() {
        composeTestRule.setContent {
            InProgressActionsBottomSheet(
                onCancelAllTransfers = onCancelAllTransfers,
            )
        }
    }
}