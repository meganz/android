package mega.privacy.android.feature.sync.ui.createnewfolder

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.feature.sync.ui.createnewfolder.model.CreateNewFolderState
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.ui.res.stringResource
import de.palm.composestateevents.EventEffect

@Composable
internal fun CreateNewFolderDialog(
    currentFolder: Node,
    onSuccess: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: CreateNewFolderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    EventEffect(
        event = uiState.validNameConfirmed,
        onConsumed = viewModel::resetValidNameConfirmedEvent,
        action = onSuccess,
    )
    CreateNewFolderDialogBody(
        uiState = uiState,
        onConfirm = { newFolderName ->
            viewModel.checkIsValidName(
                newFolderName = newFolderName,
                parentNode = currentFolder,
            )
        },
        onCancel = {
            viewModel.clearErrorMessage()
            onCancel()
        },
        onInputChange = { viewModel.clearErrorMessage() },
    )
}

@Composable
private fun CreateNewFolderDialogBody(
    uiState: CreateNewFolderState,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    onInputChange: (String) -> Unit = {},
) {
    InputDialog(
        title = stringResource(sharedR.string.create_new_folder_dialog_title),
        confirmButtonText = stringResource(sharedR.string.create_new_folder_dialog_confirm_button),
        cancelButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onConfirm = onConfirm,
        onDismiss = onCancel,
        modifier = Modifier.testTag(TEST_TAG_CREATE_NEW_FOLDER_DIALOG),
        hint = stringResource(sharedR.string.create_new_folder_dialog_hint_text),
        error = uiState.errorMessage?.let { nonNullErrorMessage ->
            if (nonNullErrorMessage == sharedR.string.general_invalid_characters_defined) {
                stringResource(nonNullErrorMessage).replace(
                    oldValue = "%1\$s",
                    newValue = NODE_NAME_INVALID_CHARACTERS
                )
            } else {
                stringResource(nonNullErrorMessage)
            }
        },
        onInputChange = onInputChange,
    )
}

@CombinedThemePreviews
@Composable
private fun CreateNewFolderDialogPreview(
    @PreviewParameter(CreateNewFolderDialogPreviewProvider::class) createNewFolderState: CreateNewFolderState,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CreateNewFolderDialogBody(
            uiState = createNewFolderState,
            onConfirm = {},
            onCancel = {},
        )
    }
}

/**
 * A class that provides Preview Parameters for the [CreateNewFolderDialog]
 */
private class CreateNewFolderDialogPreviewProvider :
    PreviewParameterProvider<CreateNewFolderState> {
    override val values: Sequence<CreateNewFolderState>
        get() = sequenceOf(
            CreateNewFolderState(),
            CreateNewFolderState(errorMessage = sharedR.string.create_new_folder_dialog_error_message_empty_folder_name),
            CreateNewFolderState(errorMessage = sharedR.string.create_new_folder_dialog_error_existing_folder),
            CreateNewFolderState(errorMessage = sharedR.string.general_invalid_characters_defined),
        )
}

private const val NODE_NAME_INVALID_CHARACTERS = "\" * / : < > ? \\ |"

/**
 * Test tag for the Create New Folder Dialog
 */
internal const val TEST_TAG_CREATE_NEW_FOLDER_DIALOG = "input_dialog:create_new_folder_dialog"