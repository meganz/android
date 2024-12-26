package mega.privacy.android.app.presentation.documentscanner.groups

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A Composable Group allowing Users to change the filename of the scanned Document/s
 *
 * @param filename The file of the resulting scan/s
 * @param filenameErrorMessage The error message shown in the filename input
 * @param scanFileType The Scan File Type to determine the File Image Type shown
 * @param onFilenameChanged Lambda when the filename changes
 * @param onFilenameConfirmed Lambda when the filename is accepted by the User, triggered by the
 * [ImeAction.Done] Keyboard Button
 * @param modifier The default Modifier
 */
@Composable
internal fun SaveScannedDocumentsFilenameGroup(
    filename: String,
    @StringRes filenameErrorMessage: Int?,
    scanFileType: ScanFileType,
    onFilenameChanged: (String) -> Unit,
    onFilenameConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // Used to de-select the text when the TextField is focused and the User selects the Text again
    var keepWholeSelection by rememberSaveable { mutableStateOf(false) }
    if (keepWholeSelection) {
        SideEffect { keepWholeSelection = false }
    }

    var filenameValueState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(filename))
    }

    Column(modifier = modifier) {
        MegaText(
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                )
                .testTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_HEADER),
            text = stringResource(R.string.scan_file_name),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.body2,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 36.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier
                    .wrapContentWidth()
                    .testTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILE_TYPE_IMAGE),
                imageVector = ImageVector.vectorResource(scanFileType.iconRes),
                contentDescription = "Image File Type"
            )
            GenericTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            // When the Text Field gains focus, select the entire filename
                            filenameValueState = filenameValueState.copy(
                                selection = TextRange(0, filenameValueState.text.length)
                            )
                            keepWholeSelection = true
                        } else {
                            // When the Text Field loses focus, de-select the entire filename
                            filenameValueState = filenameValueState.copy(
                                selection = TextRange(0, 0)
                            )
                        }
                    }
                    .testTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILENAME_TEXT_FIELD),
                textFieldValue = if (isFocused) {
                    filenameValueState
                } else {
                    TextFieldValue("${filenameValueState.text}${scanFileType.fileSuffix}")
                },
                placeholder = "",
                onTextChange = { newTextFieldValue ->
                    onFilenameChanged(newTextFieldValue.text)

                    if (keepWholeSelection) {
                        keepWholeSelection = false
                    } else {
                        filenameValueState = newTextFieldValue
                    }
                },
                errorText = filenameErrorMessage?.let { stringResource(it) },
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus(true)
                        onFilenameConfirmed(filenameValueState.text)
                    }
                ),
            )

            Image(
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable(enabled = !isFocused) { focusRequester.requestFocus() }
                    .testTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_EDIT_FILENAME_IMAGE)
                    .alpha(if (isFocused) 0f else 1f),
                painter = painterResource(IconPackR.drawable.ic_edit_medium_regular_outline),
                contentDescription = "Edit Filename Image"
            )
        }
    }
}

/**
 * A Preview Composable for [SaveScannedDocumentsFilenameGroup] that shows the different File Image
 * types
 *
 * @param scanFileType The specific Scan File Type
 */
@CombinedThemePreviews
@Composable
private fun SaveScannedDocumentsFilenameGroupFileImagePreview(
    @PreviewParameter(ScanFileTypeProvider::class) scanFileType: ScanFileType,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsFilenameGroup(
            filename = "Scanned_file",
            filenameErrorMessage = null,
            scanFileType = scanFileType,
            onFilenameChanged = {},
            onFilenameConfirmed = {},
        )
    }
}

private class ScanFileTypeProvider : PreviewParameterProvider<ScanFileType> {
    override val values: Sequence<ScanFileType>
        get() = ScanFileType.entries.asSequence()
}

/**
 * A Preview Composable for [SaveScannedDocumentsFilenameGroupFileImagePreview] that shows the
 * different input Error Messages
 *
 * @param filenameInputError The Filename Input Error object to simulate the errors
 */
@CombinedThemePreviews
@Composable
private fun SaveScannedDocumentsFilenameGroupInputErrorPreview(
    @PreviewParameter(FilenameInputErrorProvider::class) filenameInputError: FilenameInputError,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsFilenameGroup(
            filename = filenameInputError.filename,
            filenameErrorMessage = filenameInputError.filenameErrorMessage,
            scanFileType = ScanFileType.Pdf,
            onFilenameChanged = {},
            onFilenameConfirmed = {},
        )
    }
}

private data class FilenameInputError(
    val filename: String,
    @StringRes val filenameErrorMessage: Int?,
)

private class FilenameInputErrorProvider : PreviewParameterProvider<FilenameInputError> {
    override val values: Sequence<FilenameInputError>
        get() = sequenceOf(
            FilenameInputError(
                filename = "",
                filenameErrorMessage = R.string.scan_incorrect_name,
            ),
            FilenameInputError(
                filename = "Scanned_file?",
                filenameErrorMessage = R.string.scan_invalid_characters,
            ),
        )
}

internal const val SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_HEADER =
    "save_scanned_documents_filename_group:mega_text_header"
internal const val SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILE_TYPE_IMAGE =
    "save_scanned_documents_filename_group:image_file_type"
internal const val SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILENAME_TEXT_FIELD =
    "save_scanned_documents_filename_group:generic_text_field_filename"
internal const val SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_EDIT_FILENAME_IMAGE =
    "save_scanned_documents_filename_group:edit_filename_image"