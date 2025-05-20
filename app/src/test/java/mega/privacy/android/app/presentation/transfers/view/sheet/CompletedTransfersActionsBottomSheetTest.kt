package mega.privacy.android.app.presentation.transfers.view.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class CompletedTransfersActionsBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onClearAllTransfers = mock<() -> Unit>()
    private val onDismissSheet = mock<() -> Unit>()

    @Test
    fun `test that sheet shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_COMPLETED_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CLEAR_ALL_COMPLETED_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.option_to_clear_transfers).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on clear all transfers option invokes onClearAllTransfers and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_CLEAR_ALL_COMPLETED_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onClearAllTransfers).invoke()
        verify(onDismissSheet).invoke()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule() {
        composeTestRule.setContent {
            CompletedTransfersActionsBottomSheet(
                onClearAllTransfers = onClearAllTransfers,
                onDismissSheet = onDismissSheet,
            )
        }
    }
}