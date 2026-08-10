package mega.privacy.android.shared.nodes.dialog.newfile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.privacy.android.domain.exception.DotNameException
import mega.privacy.android.domain.exception.DoubleDotNameException
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeExtensionException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.exception.NodeNameException
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.nodes.dialog.newfolder.INVALID_CHARACTERS
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Stateless dialog for creating a new text-like file (e.g. a `.txt` text file or a `.url`
 * Internet Shortcut). The caller (typically a destination wired to
 * [NewTextFileNodeDialogViewModel]) owns the state and provides the callbacks.
 *
 * When the validation flow in the caller succeeds, the caller emits a triggered
 * [NewTextFileNodeDialogUiState.validationSuccessEvent], the dialog consumes it via
 * [onValidationSuccessEventConsumed] and forwards the trimmed file name through [onConfirm]
 * so the caller can perform the positive action (open the text editor, upload, etc.).
 *
 * @param uiState Current state: file name input, validation error, and the one-shot success event.
 * @param title Dialog title. Defaults to "New text file".
 * @param onConfirm Invoked with the trimmed file name when the success event fires.
 * @param onFileNameChanged Invoked with the new text whenever the user edits the input.
 * @param validateFileName Invoked when the user presses the positive button.
 * @param onValidationSuccessEventConsumed Invoked after [onConfirm] runs so the caller can clear
 *  [NewTextFileNodeDialogUiState.validationSuccessEvent].
 * @param modifier Modifier applied to the dialog container.
 * @param onDismiss Invoked when the user cancels or dismisses the dialog.
 */
@Composable
fun NewTextFileNodeDialog(
    uiState: NewTextFileNodeDialogUiState,
    title: String,
    onConfirm: (fileName: String) -> Unit,
    onFileNameChanged: (fileName: String) -> Unit,
    validateFileName: () -> Unit,
    onValidationSuccessEventConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    when (uiState) {
        is NewTextFileNodeDialogUiState.Data -> {
            var fileNameInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(uiState.fileName, TextRange(0)))
            }

            EventEffect(
                event = uiState.validationSuccessEvent,
                onConsumed = onValidationSuccessEventConsumed,
            ) { fileName ->
                onConfirm(fileName)
            }

            BasicInputDialog(
                title = title,
                modifier = modifier.testTag(NEW_TEXT_FILE_NODE_DIALOG_TAG),
                inputValue = fileNameInput,
                onValueChange = { newValue ->
                    fileNameInput = newValue
                    onFileNameChanged(newValue.text)
                },
                isAutoShowKeyboard = true,
                errorText = uiState.fileNameException?.text(),
                positiveButtonText = stringResource(id = sharedR.string.general_create_label),
                onPositiveButtonClicked = validateFileName,
                negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onNegativeButtonClicked = onDismiss,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                onDismiss = onDismiss,
            )
        }

        NewTextFileNodeDialogUiState.Loading -> {
            //It will be quick enough. No loading view required.
        }
    }
}

@Composable
private fun NodeNameException.text(): String = when (this) {
    is EmptyNodeNameException -> stringResource(sharedR.string.general_invalid_string)
    is DotNameException -> stringResource(sharedR.string.general_invalid_dot_name_warning)
    is DoubleDotNameException -> stringResource(sharedR.string.general_invalid_double_dot_name_warning)
    is InvalidNodeNameException -> stringResource(
        sharedR.string.general_invalid_characters_defined, INVALID_CHARACTERS
    )

    is NodeNameAlreadyExistsException -> stringResource(NodesR.string.same_file_name_warning)
    is InvalidNodeExtensionException -> stringResource(sharedR.string.new_text_file_invalid_extension_error_message)
}

internal const val NEW_TEXT_FILE_NODE_DIALOG_TAG = "new_text_file_node:dialog"
