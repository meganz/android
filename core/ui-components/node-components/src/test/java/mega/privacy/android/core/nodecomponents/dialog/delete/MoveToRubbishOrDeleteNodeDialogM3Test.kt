package mega.privacy.android.core.nodecomponents.dialog.delete

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class MoveToRubbishOrDeleteNodeDialogM3Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val viewModel: MoveToRubbishOrDeleteNodeDialogViewModel = mock()

    @Test
    fun `test that positive button click invokes onConfirm and not onDismiss in move-to-rubbish mode`() {
        val onConfirm = mock<() -> Unit>()
        val onDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            MoveToRubbishOrDeleteNodeDialogM3(
                nodes = listOf(1L),
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                viewModel = viewModel,
                isNodeInRubbish = false,
            )
        }

        composeTestRule.onNodeWithTag(MOVE_TO_RUBBISH_OR_DELETE_NODE_DIALOG_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.general_move))
            .performClick()

        verify(onConfirm).invoke()
        verify(onDismiss, never()).invoke()
        verify(viewModel).moveNodesToRubbishBin(listOf(1L))
    }

    @Test
    fun `test that positive button click invokes onConfirm and not onDismiss in delete-permanently mode`() {
        val onConfirm = mock<() -> Unit>()
        val onDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            MoveToRubbishOrDeleteNodeDialogM3(
                nodes = listOf(1L),
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                viewModel = viewModel,
                isNodeInRubbish = true,
            )
        }

        composeTestRule.onNodeWithTag(MOVE_TO_RUBBISH_OR_DELETE_NODE_DIALOG_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(NodesR.string.rubbish_bin_delete_confirmation_dialog_button_delete)
            )
            .performClick()

        verify(onConfirm).invoke()
        verify(onDismiss, never()).invoke()
        verify(viewModel).deleteNodes(listOf(1L))
    }

    @Test
    fun `test that negative button click invokes onDismiss and not onConfirm`() {
        val onConfirm = mock<() -> Unit>()
        val onDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            MoveToRubbishOrDeleteNodeDialogM3(
                nodes = listOf(1L),
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                viewModel = viewModel,
                isNodeInRubbish = false,
            )
        }

        composeTestRule.onNodeWithTag(MOVE_TO_RUBBISH_OR_DELETE_NODE_DIALOG_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(sharedResR.string.general_dialog_cancel_button))
            .performClick()

        verify(onDismiss).invoke()
        verify(onConfirm, never()).invoke()
    }
}
