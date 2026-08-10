package mega.privacy.android.shared.nodes.dialog.removelink

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class RemoveNodeLinkDialogM3Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val viewModel: RemoveNodeLinkViewModel = mock()

    private fun string(id: Int): String = context.getString(id)

    @Test
    fun `test that the dialog and its buttons are shown`() {
        composeTestRule.setContent {
            RemoveNodeLinkDialogM3(nodes = nodes, onDismiss = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag(REMOVE_NODE_LINK_DIALOG_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.remove_links_warning_message))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.general_remove)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the positive button disables the export and dismisses the dialog`() {
        val onDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            RemoveNodeLinkDialogM3(nodes = nodes, onDismiss = onDismiss, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText(string(sharedR.string.general_remove)).performClick()

        verify(viewModel).disableExport(nodes)
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that the negative button dismisses the dialog without disabling the export`() {
        val onDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            RemoveNodeLinkDialogM3(nodes = nodes, onDismiss = onDismiss, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .performClick()

        verify(onDismiss).invoke()
        verify(viewModel, never()).disableExport(any())
    }

    private companion object {
        val nodes = listOf(1L, 2L)
    }
}
