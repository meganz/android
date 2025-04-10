package mega.privacy.android.feature.sync.ui.renamebackup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.consumed
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.feature.sync.ui.renamebackup.model.RENAME_AND_CREATE_BACKUP_DIALOG_TAG
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupDialog
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupState
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Test class for [RenameAndCreateBackupDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class RenameAndCreateBackupDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val renameAndCreateBackupViewModel = mock<RenameAndCreateBackupViewModel>()

    @Test
    fun `test that the rename and create backup dialog is shown`() {
        val uiState = RenameAndCreateBackupState(
            successEvent = consumed,
        )

        renameAndCreateBackupViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(uiState))
        }
        composeTestRule.setContent {
            RenameAndCreateBackupDialog(
                backupName = "Backup name",
                localPath = "Local Path",
                onSuccess = {},
                onCancel = {},
                renameAndCreateBackupViewModel = renameAndCreateBackupViewModel,
            )
        }
        composeTestRule.onNodeWithTag(RENAME_AND_CREATE_BACKUP_DIALOG_TAG).assertIsDisplayed()
    }
}