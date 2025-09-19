package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.fromId
import mega.privacy.android.app.fromPluralId
import mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderState
import mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderViewModel
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class RemoveToolbarMenuItemShareFolderDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel = mock<RemoveShareFolderViewModel>()

    @Test
    fun `test that when multiple folders are shared with only one contact`() {
        viewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    RemoveShareFolderState(
                        numberOfShareContact = 1,
                        numberOfShareFolder = 2,
                    )
                )
            )
        }

        composeTestRule.setContent {
            RemoveShareFolderDialog(
                nodeList = listOf(NodeId(1234L), NodeId(2345L)),
                viewModel = viewModel,
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText(fromId(sharedR.string.stop_sharing_dialog_title_plurals)).assertExists()

        composeTestRule.onNodeWithText(
            fromId(sharedR.string.stop_sharing_dialog_positive_button_text)
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(sharedR.string.general_dialog_cancel_button)
        ).assertExists()
    }

    @Test
    fun `test that when single folders are shared with only one contact`() {
        viewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    RemoveShareFolderState(
                        numberOfShareContact = 1,
                        numberOfShareFolder = 1,
                    )
                )
            )
        }

        composeTestRule.setContent {
            RemoveShareFolderDialog(
                nodeList = listOf(NodeId(1234L), NodeId(2345L)),
                viewModel = viewModel,
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText(fromId(sharedR.string.stop_sharing_dialog_title)).assertExists()

        composeTestRule.onNodeWithText(
            fromId(sharedR.string.stop_sharing_dialog_positive_button_text)
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(sharedR.string.general_dialog_cancel_button)
        ).assertExists()
    }

    @Test
    fun `test that when single folders are shared with multiple contacts`() {
        viewModel.stub {
            on { state }.thenReturn(
                MutableStateFlow(
                    RemoveShareFolderState(
                        numberOfShareContact = 2,
                        numberOfShareFolder = 1,
                    )
                )
            )
        }

        composeTestRule.setContent {
            RemoveShareFolderDialog(
                nodeList = listOf(NodeId(1234L), NodeId(2345L)),
                viewModel = viewModel,
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText(fromId(sharedR.string.stop_sharing_dialog_title)).assertExists()

        composeTestRule.onNodeWithText(
            fromId(sharedR.string.stop_sharing_dialog_positive_button_text)
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(sharedR.string.general_dialog_cancel_button)
        ).assertExists()
    }
}