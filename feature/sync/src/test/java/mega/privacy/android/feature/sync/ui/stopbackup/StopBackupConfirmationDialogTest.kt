package mega.privacy.android.feature.sync.ui.stopbackup

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.CONFIRM_TAG
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.TITLE_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StopBackupConfirmationDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun initComposeRuleContent() {
        composeRule.setContent {
            StopBackupConfirmationDialog(
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @Test
    fun `test that all dialog items are displayed and clickable if required`() {
        initComposeRuleContent()
        composeRule.onNodeWithTag(TITLE_TAG).assertIsDisplayed().assertHasNoClickAction()
        composeRule.onNodeWithTag(STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG).assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithTag(STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_ROW_TEST_TAG)
            .assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(
            STOP_BACKUP_CONFIRMATION_DIALOG_MOVE_OPTION_SELECT_DESTINATION_TEST_TAG,
            useUnmergedTree = true,
        ).assertIsDisplayed().assertHasNoClickAction()
        composeRule.onNodeWithTag(STOP_BACKUP_CONFIRMATION_DIALOG_DELETE_OPTION_ROW_TEST_TAG)
            .assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(CANCEL_TAG).assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag(CONFIRM_TAG).assertIsDisplayed().assertHasClickAction()
    }
}