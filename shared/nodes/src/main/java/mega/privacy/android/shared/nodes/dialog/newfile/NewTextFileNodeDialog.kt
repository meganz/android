package mega.privacy.android.shared.nodes.dialog.newfile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.components.inputfields.TextInputField
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.exception.DotNameException
import mega.privacy.android.domain.exception.DoubleDotNameException
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeExtensionException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.exception.NodeNameException
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.DEFAULT_TEXT_FILE_EXTENSION
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.RICH_TEXT_FILE_EXTENSION
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

            val onInputChanged: (TextFieldValue) -> Unit = { newValue ->
                fileNameInput = newValue
                onFileNameChanged(newValue.text)
            }
            if (uiState.showRichTextOption) {
                NewTextFileDialogWithRichTextOption(
                    title = title,
                    fileNameInput = fileNameInput,
                    onInputChanged = onInputChanged,
                    errorText = uiState.fileNameException?.text(),
                    validateFileName = validateFileName,
                    onDismiss = onDismiss,
                    modifier = modifier,
                )
            } else {
                BasicInputDialog(
                    title = title,
                    modifier = modifier.testTag(NEW_TEXT_FILE_NODE_DIALOG_TAG),
                    inputValue = fileNameInput,
                    onValueChange = onInputChanged,
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
        }

        NewTextFileNodeDialogUiState.Loading -> {
            //It will be quick enough. No loading view required.
        }
    }
}

/**
 * The dialog variant with the "Rich text" checkbox (built on [BasicDialog] because
 * [BasicInputDialog] has no slot below the input). The checkbox is a convenience view over the
 * name field, which stays the single source of truth: checking swaps the extension to `.md`,
 * unchecking restores `.txt`, and typing any other extension simply unchecks it — free-form
 * extensions keep working exactly as without the checkbox.
 */
@Composable
private fun NewTextFileDialogWithRichTextOption(
    title: String,
    fileNameInput: TextFieldValue,
    onInputChanged: (TextFieldValue) -> Unit,
    errorText: String?,
    validateFileName: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        title = title,
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onClick = onDismiss,
            ),
            BasicDialogButton(
                text = stringResource(id = sharedR.string.general_create_label),
                onClick = validateFileName,
            ),
        ),
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(NEW_TEXT_FILE_NODE_DIALOG_TAG),
    ) {
        Column {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                runCatching { focusRequester.requestFocus() }
            }
            TextInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardType = KeyboardType.Text,
                textFieldValue = fileNameInput,
                capitalization = KeyboardCapitalization.Sentences,
                errorText = errorText,
                onValueChanged = onInputChanged,
            )
            val isRichText = fileNameInput.text.trim()
                .endsWith(RICH_TEXT_FILE_EXTENSION, ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isRichText,
                        role = Role.Checkbox,
                        onValueChange = { checked ->
                            val newName = fileNameInput.text.withFileExtension(
                                if (checked) RICH_TEXT_FILE_EXTENSION else DEFAULT_TEXT_FILE_EXTENSION,
                            )
                            onInputChanged(
                                TextFieldValue(
                                    text = newName,
                                    selection = TextRange(
                                        fileNameInput.selection.start.coerceAtMost(newName.length),
                                    ),
                                ),
                            )
                        },
                    )
                    .padding(top = 16.dp)
                    .testTag(NEW_TEXT_FILE_RICH_TEXT_CHECKBOX_TAG),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isRichText,
                    onCheckStateChanged = {},
                    clickable = false,
                    tapTargetArea = false,
                )
                MegaText(
                    text = stringResource(id = sharedR.string.new_text_file_rich_text_formatting_label),
                    textColor = TextColor.Primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/**
 * Replaces the name's trailing `.ext` segment with [extension], or appends it when the name
 * has none. The swap is visible in the field immediately, so an unexpected replacement (e.g.
 * a multi-dot name) is transparent and trivially correctable.
 */
internal fun String.withFileExtension(extension: String): String {
    val dot = lastIndexOf('.')
    val base = if (dot >= 0) substring(0, dot) else this
    return base + extension
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
internal const val NEW_TEXT_FILE_RICH_TEXT_CHECKBOX_TAG = "new_text_file_node:rich_text_checkbox"
