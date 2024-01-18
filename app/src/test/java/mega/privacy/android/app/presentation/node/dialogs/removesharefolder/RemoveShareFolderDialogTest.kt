package mega.privacy.android.app.presentation.node.dialogs.removesharefolder

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.fromPluralId

@RunWith(AndroidJUnit4::class)
class RemoveShareFolderDialogTest {

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
        composeTestRule.onNodeWithText(
            fromPluralId(
                R.plurals.alert_remove_several_shares,
                2
            )
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(R.string.shared_items_outgoing_unshare_confirm_dialog_button_yes)
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(R.string.shared_items_outgoing_unshare_confirm_dialog_button_no)
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
        composeTestRule.onNodeWithText(
            fromPluralId(
                R.plurals.confirmation_remove_outgoing_shares,
                1
            )
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(R.string.general_remove)
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(R.string.general_cancel)
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
        composeTestRule.onNodeWithText(
            fromPluralId(
                R.plurals.confirmation_remove_outgoing_shares,
                2
            )
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(R.string.general_remove)
        ).assertExists()

        composeTestRule.onNodeWithText(
            fromId(R.string.general_cancel)
        ).assertExists()
    }
}