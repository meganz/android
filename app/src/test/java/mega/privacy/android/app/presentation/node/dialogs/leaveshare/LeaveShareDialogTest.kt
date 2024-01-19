package mega.privacy.android.app.presentation.node.dialogs.leaveshare

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.fromPluralId

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
            fromPluralId(
                R.plurals.confirmation_leave_share_folder,
                2
            )
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
            fromPluralId(
                R.plurals.confirmation_leave_share_folder,
                1
            )
        ).assertExists()
    }
}