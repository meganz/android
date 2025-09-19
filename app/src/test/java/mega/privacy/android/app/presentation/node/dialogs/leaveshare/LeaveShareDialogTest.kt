package mega.privacy.android.app.presentation.node.dialogs.leaveshare

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.fromId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import mega.privacy.android.app.fromPluralId
import mega.privacy.android.core.nodecomponents.dialog.leaveshare.LeaveShareDialogViewModel
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class LeaveShareDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val leaveShareDialogViewModel = mock<LeaveShareDialogViewModel>()

    @Test
    fun `test that when multiple folders are to be removed`() {
        composeTestRule.setContent {
            LeaveShareDialog(
                handles = listOf(1234L, 2345L),
                leaveShareDialogViewModel = leaveShareDialogViewModel,
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText(
            fromId(sharedR.string.leave_shared_folder_confirmation_message)
        ).assertExists()
    }

    @Test
    fun `test that when single folder are to be removed`() {
        composeTestRule.setContent {
            LeaveShareDialog(
                handles = listOf(1234L),
                leaveShareDialogViewModel = leaveShareDialogViewModel,
                onDismiss = {}
            )
        }
        composeTestRule.onNodeWithText(
            fromId(sharedR.string.leave_shared_folder_confirmation_message)
        ).assertExists()
    }
}