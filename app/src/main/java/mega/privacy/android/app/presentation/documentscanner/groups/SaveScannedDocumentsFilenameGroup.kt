package mega.privacy.android.app.presentation.documentscanner.groups

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.domain.entity.documentscanner.ScanFilenameValidationStatus
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.accent_900_accent_050
import mega.privacy.android.shared.resources.R as SharedR

/**
 * A Composable Group allowing Users to change the filename of the scanned Document/s
 *
 * @param filename The file of the resulting scan/s
 * @param filenameValidationStatus The filename validation status
 * @param scanFileType The Scan File Type to determine the File Image Type shown
 * @param onFilenameChanged Lambda when the filename changes
 * @param onFilenameConfirmed Lambda when the filename is accepted by the User, triggered by the
 * [ImeAction.Done] Keyboard Button
 * @param modifier The default Modifier
 */
@Composable
internal fun SaveScannedDocumentsFilenameGroup(
    filename: String,
    filenameValidationStatus: ScanFilenameValidationStatus?,
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

    LaunchedEffect(filename) {
        if (filename != filenameValueState.text) {
            filenameValueState = TextFieldValue(filename)
        }
    }

    Column(modifier = modifier) {
        MegaText(
            modifier = Modifier
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .testTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_HEADER),
            text = stringResource(R.string.scan_file_name),
            textColor = TextColor.Secondary,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Image(
                modifier = Modifier
                    .testTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILE_TYPE_IMAGE),
                imageVector = ImageVector.vectorResource(scanFileType.iconRes),
                contentDescription = "Image File Type"
            )
            GenericTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp, start = 12.dp, end = 8.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {

                            val fileSuffix = scanFileType.fileSuffix
                            val lastFileSuffixIndex = filename.lastIndexOf(fileSuffix)

                            filenameValueState = filenameValueState.copy(
                                selection = if (lastFileSuffixIndex != -1 && lastFileSuffixIndex == filename.length - fileSuffix.length) {
                                    // If the filename contains the expected suffix, highlight the filename without the suffix
                                    TextRange(0, lastFileSuffixIndex)
                                } else {
                                    // Else, highlight the entire filename
                                    TextRange(0, filename.length)
                                }
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
                textFieldValue = filenameValueState,
                placeholder = "",
                showIndicatorLine = isFocused || (filenameValidationStatus != null && filenameValidationStatus != ScanFilenameValidationStatus.ValidFilename),
                onTextChange = { newTextFieldValue ->
                    onFilenameChanged(newTextFieldValue.text)

                    if (keepWholeSelection) {
                        keepWholeSelection = false
                    } else {
                        filenameValueState = newTextFieldValue
                    }
                },
                errorText = getFilenameErrorMessage(
                    filenameValidationStatus = filenameValidationStatus,
                    scanFileType = scanFileType,
                ),
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
                    .padding(top = 12.dp)
                    .clickable(enabled = !isFocused) { focusRequester.requestFocus() }
                    .testTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_EDIT_FILENAME_IMAGE)
                    .alpha(if (isFocused) 0f else 1f),
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Pen2),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.accent_900_accent_050),
                contentDescription = "Edit Filename Image"
            )
        }
    }
}

/**
 * Retrieves the correct error message when an incorrect filename is supplied
 *
 * @param filenameValidationStatus The filename validation status
 * @param scanFileType The scan file type
 *
 * @return The error message to be displayed in the filename input
 */
@Composable
private fun getFilenameErrorMessage(
    filenameValidationStatus: ScanFilenameValidationStatus?,
    scanFileType: ScanFileType,
) = when (filenameValidationStatus) {
    ScanFilenameValidationStatus.EmptyFilename -> stringResource(R.string.scan_incorrect_name)
    ScanFilenameValidationStatus.InvalidFilename -> stringResource(
        R.string.scan_snackbar_invalid_characters,
        "\" * / : < > ? \\ |"
    )

    ScanFilenameValidationStatus.MissingFilenameExtension,
    ScanFilenameValidationStatus.IncorrectFilenameExtension,
        -> stringResource(
        SharedR.string.document_scanning_settings_missing_file_extension_error_message,
        scanFileType.fileSuffix,
    )

    else -> null
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsFilenameGroup(
            filename = "Scanned_file.pdf",
            filenameValidationStatus = ScanFilenameValidationStatus.ValidFilename,
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsFilenameGroup(
            filename = filenameInputError.filename,
            filenameValidationStatus = filenameInputError.filenameValidationStatus,
            scanFileType = ScanFileType.Pdf,
            onFilenameChanged = {},
            onFilenameConfirmed = {},
        )
    }
}

private data class FilenameInputError(
    val filename: String,
    val filenameValidationStatus: ScanFilenameValidationStatus?,
)

private class FilenameInputErrorProvider : PreviewParameterProvider<FilenameInputError> {
    override val values: Sequence<FilenameInputError>
        get() = sequenceOf(
            FilenameInputError(
                filename = "Scanned_file",
                filenameValidationStatus = ScanFilenameValidationStatus.MissingFilenameExtension,
            ),
            FilenameInputError(
                filename = "",
                filenameValidationStatus = ScanFilenameValidationStatus.EmptyFilename,
            ),
            FilenameInputError(
                filename = "Scanned_f\\le.pdf",
                filenameValidationStatus = ScanFilenameValidationStatus.InvalidFilename,
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