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
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ActiveTransfersActionsBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onSelectTransfers = mock<() -> Unit>()
    private val onCancelAllTransfers = mock<() -> Unit>()
    private val onDismissSheet = mock<() -> Unit>()

    @Test
    fun `test that sheet shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_ACTIVE_ACTIONS_PANEL).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_SELECT_ACTION).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_select).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CANCEL_ALL_ACTION).assertIsDisplayed()
            onNodeWithText(R.string.menu_cancel_all_transfers).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on select transfers option invokes onSelectTransfers and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_SELECT_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onSelectTransfers).invoke()
        verify(onDismissSheet).invoke()
    }


    @Test
    fun `test that clicking on cancel all transfers option invokes onCancelAllTransfers and onDismissSheet`() {
        initComposeTestRule()

        composeTestRule.onNodeWithTag(TEST_TAG_CANCEL_ALL_ACTION)
            .performSemanticsAction(SemanticsActions.OnClick)

        verify(onCancelAllTransfers).invoke()
        verify(onDismissSheet).invoke()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun initComposeTestRule() {
        composeTestRule.setContent {
            ActiveTransfersActionsBottomSheet(
                onSelectTransfers = onSelectTransfers,
                onCancelAllTransfers = onCancelAllTransfers,
                onDismissSheet = onDismissSheet,
            )
        }
    }
}