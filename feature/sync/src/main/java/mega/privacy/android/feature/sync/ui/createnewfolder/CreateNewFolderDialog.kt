package mega.privacy.android.feature.sync.ui.createnewfolder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.feature.sync.ui.createnewfolder.model.CreateNewFolderState
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

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
internal fun CreateNewFolderDialogBody(
    uiState: CreateNewFolderState,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    onInputChange: (String) -> Unit = {},
) {
    var folderName by rememberSaveable { mutableStateOf("") }

    BasicInputDialog(
        modifier = Modifier.testTag(TEST_TAG_CREATE_NEW_FOLDER_DIALOG),
        title = stringResource(sharedR.string.general_new_folder),
        inputValue = folderName,
        onValueChange = {
            folderName = it
            onInputChange(it)
        },
        placeholder = stringResource(sharedR.string.create_new_folder_dialog_hint_text),
        errorText = uiState.errorMessage?.let { nonNullErrorMessage ->
            if (nonNullErrorMessage == sharedR.string.general_invalid_characters_defined) {
                stringResource(nonNullErrorMessage).replace(
                    oldValue = "%1\$s",
                    newValue = NODE_NAME_INVALID_CHARACTERS
                )
            } else {
                stringResource(nonNullErrorMessage)
            }
        },
        positiveButtonText = stringResource(sharedR.string.general_create_label),
        onPositiveButtonClicked = { onConfirm(folderName) },
        negativeButtonText = stringResource(sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onCancel,
        onDismiss = onCancel,
    )
}

@CombinedThemePreviews
@Composable
private fun CreateNewFolderDialogPreview(
    @PreviewParameter(CreateNewFolderDialogPreviewProvider::class) createNewFolderState: CreateNewFolderState,
) {
    AndroidThemeForPreviews {
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