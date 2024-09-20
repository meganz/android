package mega.privacy.android.app.presentation.documentscanner

import android.net.Uri
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.groups.SaveScannedDocumentsDestinationGroup
import mega.privacy.android.app.presentation.documentscanner.groups.SaveScannedDocumentsFileTypeGroup
import mega.privacy.android.app.presentation.documentscanner.groups.SaveScannedDocumentsFilenameGroup
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar

/**
 * A Composable that holds views displaying the main Save Scanned Documents screen
 *
 * @param uiState The Save Scanned Documents UI State
 * @param onFilenameChanged Lambda when the filename changes
 * @param onFilenameConfirmed Lambda when the filename is accepted by the User, triggered by the
 * ImeAction.Done Keyboard Button
 * @param onSaveButtonClicked Lambda when the Save button is clicked
 * @param onScanDestinationSelected Lambda when a new Scan Destination is selected
 * @param onScanFileTypeSelected Lambda when a new Scan File Type is selected
 * @param onSnackbarMessageConsumed Lambda when the Snackbar has been shown with the specific message
 * @param onUploadScansStarted Lambda to indicate that the scanned document/s should begin uploading
 * @param onUploadScansEventConsumed Lambda when the State Event to upload the scanned document/s has
 * been triggered
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SaveScannedDocumentsView(
    uiState: SaveScannedDocumentsUiState,
    onFilenameChanged: (String) -> Unit,
    onFilenameConfirmed: (String) -> Unit,
    onSaveButtonClicked: () -> Unit,
    onScanDestinationSelected: (ScanDestination) -> Unit,
    onScanFileTypeSelected: (ScanFileType) -> Unit,
    onSnackbarMessageConsumed: () -> Unit,
    onUploadScansStarted: () -> Unit,
    onUploadScansEventConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    EventEffect(
        event = uiState.snackbarMessage,
        onConsumed = { onSnackbarMessageConsumed() },
        action = { snackbarMessage ->
            scaffoldState.snackbarHostState.showAutoDurationSnackbar(
                message = if (snackbarMessage.formatArgsText != null) {
                    context.resources.getString(
                        snackbarMessage.textRes,
                        snackbarMessage.formatArgsText,
                    )
                } else {
                    context.resources.getString(snackbarMessage.textRes)
                }
            )
        }
    )
    EventEffect(
        event = uiState.uploadScansEvent,
        onConsumed = { onUploadScansEventConsumed() },
        action = onUploadScansStarted,
    )

    MegaScaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(SAVE_SCANNED_DOCUMENTS_TOOLBAR),
                title = stringResource(R.string.scan_title_save_scan),
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                SaveScannedDocumentsFilenameGroup(
                    filename = uiState.filename,
                    filenameErrorMessage = uiState.filenameErrorMessage,
                    scanFileType = uiState.scanFileType,
                    onFilenameChanged = onFilenameChanged,
                    onFilenameConfirmed = onFilenameConfirmed,
                )
                MegaDivider(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(SAVE_SCANNED_DOCUMENTS_FILE_NAME_DIVIDER),
                    dividerType = DividerType.FullSize,
                )
                if (uiState.canSelectScanFileType) {
                    SaveScannedDocumentsFileTypeGroup(
                        selectedScanFileType = uiState.scanFileType,
                        onScanFileTypeSelected = onScanFileTypeSelected,
                    )
                    MegaDivider(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_DIVIDER),
                        dividerType = DividerType.FullSize,
                    )
                }
                SaveScannedDocumentsDestinationGroup(
                    selectedScanDestination = uiState.scanDestination,
                    onScanDestinationSelected = onScanDestinationSelected,
                )
                MegaDivider(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_DIVIDER),
                    dividerType = DividerType.FullSize,
                )
            }
            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag(SAVE_SCANNED_DOCUMENTS_SAVE_BUTTON),
                textId = R.string.scan_action_save,
                onClick = onSaveButtonClicked,
            )
        }
    }
}

/**
 * A Composable Preview for [SaveScannedDocumentsView]
 */
@CombinedThemePreviews
@Composable
private fun SaveScannedDocumentsViewPreview(
    @PreviewParameter(BooleanProvider::class) canSelectScanFileType: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsView(
            uiState = SaveScannedDocumentsUiState(
                filename = "PDF",
                scanFileType = ScanFileType.Pdf,
                soloImageUri = Uri.parse("image.jpg")
            ),
            onFilenameChanged = {},
            onFilenameConfirmed = {},
            onSaveButtonClicked = {},
            onScanFileTypeSelected = {},
            onScanDestinationSelected = {},
            onSnackbarMessageConsumed = {},
            onUploadScansStarted = {},
            onUploadScansEventConsumed = {},
        )
    }
}

internal const val SAVE_SCANNED_DOCUMENTS_TOOLBAR = "save_scanned_documents_view:mega_app_bar"
internal const val SAVE_SCANNED_DOCUMENTS_FILE_NAME_DIVIDER =
    "save_scanned_documents_view:mega_divider_file_name"
internal const val SAVE_SCANNED_DOCUMENTS_FILE_TYPE_DIVIDER =
    "save_scanned_documents_view:mega_divider_file_type"
internal const val SAVE_SCANNED_DOCUMENTS_DESTINATION_DIVIDER =
    "save_scanned_documents_view:mega_divider_destination"
internal const val SAVE_SCANNED_DOCUMENTS_SAVE_BUTTON =
    "save_scanned_documents_view:raised_default_mega_button_save"