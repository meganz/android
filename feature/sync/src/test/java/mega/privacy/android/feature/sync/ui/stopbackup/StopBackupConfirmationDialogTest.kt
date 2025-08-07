package mega.privacy.android.feature.sync.ui.stopbackup

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.TITLE_TAG
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class StopBackupConfirmationDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun initComposeRuleContent() {
        composeRule.setContent {
            StopBackupConfirmationDialogBody(
                onConfirm = { _, _ -> },
                onDismiss = {},
                onSelectStopBackupDestinationClicked = {},
                folderName = null
            )
        }
    }

    @Test
    fun `test that all dialog items are displayed and clickable if required`() {
        initComposeRuleContent()
        composeRule.onNodeWithTag(TITLE_TAG).assertIsDisplayed().assertHasNoClickAction()
        composeRule.onNodeWithTag(STOP_BACKUP_CONFIRMATION_DIALOG_BODY_TEST_TAG).assertIsDisplayed()
            .assertHasNoClickAction()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.sync_stop_backup_confirm_dialog_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.sync_stop_backup_confirm_dialog_text))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.sync_stop_backup_confirm_dialog_move_explanation))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.sync_stop_backup_confirm_dialog_delete_explanation))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.sync_stop_backup_confirm_dialog_move_cloud_drive))
            .assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.sync_stop_backup_confirm_dialog_delete_permanently))
            .assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText(composeRule.activity.getString(sharedR.string.general_dialog_cancel_button))
            .assertIsDisplayed().assertHasClickAction()
    }
}
