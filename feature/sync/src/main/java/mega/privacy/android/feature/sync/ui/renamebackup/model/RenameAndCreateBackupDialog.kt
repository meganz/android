package mega.privacy.android.feature.sync.ui.renamebackup.model

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun RenameAndCreateBackupDialog(
    backupName: String,
    localPath: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    renameAndCreateBackupViewModel: RenameAndCreateBackupViewModel = hiltViewModel(),
) {
    val uiState by renameAndCreateBackupViewModel.state.collectAsStateWithLifecycle()

    EventEffect(
        event = uiState.successEvent,
        onConsumed = renameAndCreateBackupViewModel::resetSuccessfulEvent,
        action = onSuccess,
    )
    RenameAndCreateBackupDialogBody(
        uiState = uiState,
        backupName = backupName,
        onConfirm = { newBackupName ->
            renameAndCreateBackupViewModel.renameAndCreateBackup(
                newBackupName = newBackupName,
                localPath = localPath,
            )
        },
        onDismiss = {
            renameAndCreateBackupViewModel.clearErrorMessage()
            onCancel()
        },
        onInputChange = {
            renameAndCreateBackupViewModel.clearErrorMessage()
        })
}

@Composable
internal fun RenameAndCreateBackupDialogBody(
    uiState: RenameAndCreateBackupState,
    backupName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onInputChange: () -> Unit,
) {
    var initialInput by rememberSaveable { mutableStateOf(backupName) }

    InputDialog(
        modifier = Modifier.testTag(RENAME_AND_CREATE_BACKUP_DIALOG_TAG),
        title = stringResource(sharedR.string.sync_rename_and_create_backup_dialog_title),
        message = stringResource(sharedR.string.sync_rename_and_create_backup_dialog_text),
        hint = stringResource(sharedR.string.sync_rename_and_create_backup_dialog_hint_text),
        text = initialInput,
        confirmButtonText = stringResource(sharedR.string.sync_rename_and_create_backup_dialog_positive_button),
        cancelButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onInputChange = {
            initialInput = it
            onInputChange()
        },
        error = uiState.errorMessage?.let { nonNullErrorMessage ->
            if (nonNullErrorMessage == sharedR.string.general_invalid_characters_defined) {
                stringResource(nonNullErrorMessage).replace(
                    oldValue = "%1\$s",
                    newValue = NAME_INVALID_CHARACTERS,
                )
            } else {
                stringResource(nonNullErrorMessage)
            }
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * A Preview Composable for the [RenameAndCreateBackupDialogBody]
 */
@CombinedThemePreviews
@Composable
private fun RenameAndCreateBackupDialogBodyPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RenameAndCreateBackupDialogBody(
            uiState = RenameAndCreateBackupState(),
            backupName = "Backup",
            onConfirm = {},
            onDismiss = {},
            onInputChange = {},
        )
    }
}

/**
 * A Preview Composable for the [RenameAndCreateBackupDialogBody] with name empty error
 */
@CombinedThemePreviews
@Composable
private fun RenameAndCreateBackupDialogBodyEmptyNameErrorPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RenameAndCreateBackupDialogBody(
            uiState = RenameAndCreateBackupState(errorMessage = sharedR.string.sync_rename_and_create_backup_dialog_error_message_empty_backup_name),
            backupName = "",
            onConfirm = {},
            onDismiss = {},
            onInputChange = {},
        )
    }
}

/**
 * A Preview Composable for the [RenameAndCreateBackupDialogBody] with name already exist error
 */
@CombinedThemePreviews
@Composable
private fun RenameAndCreateBackupDialogBodyNameAlreadyExistsErrorPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RenameAndCreateBackupDialogBody(
            uiState = RenameAndCreateBackupState(errorMessage = sharedR.string.sync_rename_and_create_backup_dialog_error_message_name_already_exists),
            backupName = "Backup",
            onConfirm = {},
            onDismiss = {},
            onInputChange = {},
        )
    }
}

/**
 * A Preview Composable for the [RenameAndCreateBackupDialogBody] with invalid characters error
 */
@CombinedThemePreviews
@Composable
private fun RenameAndCreateBackupDialogBodyInvalidCharactersErrorPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        RenameAndCreateBackupDialogBody(
            uiState = RenameAndCreateBackupState(errorMessage = sharedR.string.general_invalid_characters_defined),
            backupName = "Backup>",
            onConfirm = {},
            onDismiss = {},
            onInputChange = {},
        )
    }
}

private const val NAME_INVALID_CHARACTERS = "\" * / : < > ? \\ |"

/**
 * Test tag for the Rename And Create Backup Dialog
 */
internal const val RENAME_AND_CREATE_BACKUP_DIALOG_TAG =
    "rename_and_create_backup_dialog:input_dialog"
