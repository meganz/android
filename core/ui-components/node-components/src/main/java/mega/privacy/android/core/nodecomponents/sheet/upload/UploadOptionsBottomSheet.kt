package mega.privacy.android.core.nodecomponents.sheet.upload

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.icon.pack.IconPack

/**
 * Material 3 bottom sheet for home FAB options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadOptionsBottomSheet(
    onUploadFilesClicked: () -> Unit,
    onUploadFolderClicked: () -> Unit,
    onScanDocumentClicked: () -> Unit,
    onCaptureClicked: () -> Unit,
    onNewFolderClicked: () -> Unit,
    onNewTextFileClicked: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    ),
) = MegaModalBottomSheet(
    bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
    onDismissRequest = onDismissSheet,
    modifier = modifier
        .fillMaxWidth()
        .semantics { testTagsAsResourceId = true }
        .testTag(TEST_TAG_UPLOAD_OPTIONS_SHEET),
    sheetState = sheetState,
) {
    UploadOptionItem(
        text = stringResource(id = R.string.upload_files),
        icon = IconPack.Medium.Thin.Outline.FileUpload,
        testTag = TEST_TAG_UPLOAD_FILES_ACTION,
        onClick = {
            onUploadFilesClicked()
            onDismissSheet()
        },
    )
    UploadOptionItem(
        text = stringResource(id = R.string.upload_folder),
        icon = IconPack.Medium.Thin.Outline.FolderArrow,
        testTag = TEST_TAG_UPLOAD_FOLDER_ACTION,
        onClick = {
            onUploadFolderClicked()
            onDismissSheet()
        },
    )
    UploadOptionItem(
        text = stringResource(id = R.string.menu_scan_document),
        icon = IconPack.Medium.Thin.Outline.FileScan,
        testTag = TEST_TAG_SCAN_DOCUMENT_ACTION,
        onClick = {
            onScanDocumentClicked()
            onDismissSheet()
        },
    )
    UploadOptionItem(
        text = stringResource(id = R.string.menu_take_picture),
        icon = IconPack.Medium.Thin.Outline.Camera,
        testTag = TEST_TAG_CAPTURE_ACTION,
        onClick = {
            onCaptureClicked()
            onDismissSheet()
        },
    )
    UploadOptionItem(
        text = stringResource(id = R.string.menu_new_folder),
        icon = IconPack.Medium.Thin.Outline.FolderPlus01,
        testTag = TEST_TAG_NEW_FOLDER_ACTION,
        onClick = {
            onNewFolderClicked()
            onDismissSheet()
        },
    )
    UploadOptionItem(
        text = stringResource(id = R.string.action_create_txt),
        icon = IconPack.Medium.Thin.Outline.FilePlus02,
        testTag = TEST_TAG_NEW_TEXT_FILE_ACTION,
        onClick = {
            onNewTextFileClicked()
            onDismissSheet()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun CreateNewNodeBottomSheetPreview() {
    AndroidThemeForPreviews {
        UploadOptionsBottomSheet(
            onUploadFilesClicked = {},
            onUploadFolderClicked = {},
            onScanDocumentClicked = {},
            onCaptureClicked = {},
            onNewFolderClicked = {},
            onNewTextFileClicked = {},
            onDismissSheet = {},
        )
    }
}

internal const val TEST_TAG_UPLOAD_OPTIONS_SHEET = "home_fab_options_panel"
internal const val TEST_TAG_UPLOAD_FILES_ACTION = "$TEST_TAG_UPLOAD_OPTIONS_SHEET:upload_files_action"
internal const val TEST_TAG_UPLOAD_FOLDER_ACTION = "$TEST_TAG_UPLOAD_OPTIONS_SHEET:upload_folder_action"
internal const val TEST_TAG_SCAN_DOCUMENT_ACTION = "$TEST_TAG_UPLOAD_OPTIONS_SHEET:scan_document_action"
internal const val TEST_TAG_CAPTURE_ACTION = "$TEST_TAG_UPLOAD_OPTIONS_SHEET:capture_action"
internal const val TEST_TAG_NEW_FOLDER_ACTION = "$TEST_TAG_UPLOAD_OPTIONS_SHEET:new_folder_action"
internal const val TEST_TAG_NEW_TEXT_FILE_ACTION = "$TEST_TAG_UPLOAD_OPTIONS_SHEET:new_text_file_action"