package mega.privacy.android.feature.sync.ui

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.createnewfolder.CreateNewFolderDialogBody
import mega.privacy.android.feature.sync.ui.createnewfolder.model.CreateNewFolderState
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupDialogBody
import mega.privacy.android.feature.sync.ui.renamebackup.model.RenameAndCreateBackupState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The two input dialogs, each in a clean and an error state. The error states matter most: they are
 * the only ones that render the error slot, and the invalid-characters message is built by
 * substituting into the string rather than used as-is.
 */
class SyncInputDialogsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CreateNewFolderDefault() {
        AndroidThemeForPreviews {
            CreateNewFolderDialogBody(
                uiState = CreateNewFolderState(),
                onConfirm = {},
                onCancel = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CreateNewFolderInvalidCharactersError() {
        AndroidThemeForPreviews {
            CreateNewFolderDialogBody(
                uiState = CreateNewFolderState(
                    errorMessage = sharedR.string.general_invalid_characters_defined,
                ),
                onConfirm = {},
                onCancel = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RenameAndCreateBackupDefault() {
        AndroidThemeForPreviews {
            RenameAndCreateBackupDialogBody(
                uiState = RenameAndCreateBackupState(),
                backupName = "Camera uploads",
                onConfirm = {},
                onDismiss = {},
                onInputChange = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RenameAndCreateBackupEmptyNameError() {
        AndroidThemeForPreviews {
            RenameAndCreateBackupDialogBody(
                uiState = RenameAndCreateBackupState(
                    errorMessage = sharedR.string.sync_rename_and_create_backup_dialog_error_message_empty_backup_name,
                ),
                backupName = "",
                onConfirm = {},
                onDismiss = {},
                onInputChange = {},
            )
        }
    }
}
