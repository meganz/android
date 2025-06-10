package mega.privacy.android.app.presentation.transfers.view.sheet

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.view.completed.TEST_TAG_COMPLETED_TRANSFERS_VIEW
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.feature.shared.components.BottomSheetAction
import mega.privacy.android.feature.transfers.components.CompletedTransferBottomSheetHeader
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Bottom sheet for a completed transfer actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTransferActionsBottomSheet(
    completedTransfer: CompletedTransfer,
    fileTypeResId: Int?,
    previewUri: Uri?,
    onViewInFolder: () -> Unit,
    onOpenWith: () -> Unit,
    onShareLink: () -> Unit,
    onClearTransfer: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) = with(completedTransfer) {
    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissSheet,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL)
            .fillMaxWidth(),
        sheetState = sheetState,
    ) {
        CompletedTransferBottomSheetHeader(
            fileName = fileName,
            info = displayPath ?: path,
            isDownload = type.isDownloadType(),
            isError = isError,
            fileTypeResId = fileTypeResId,
            previewUri = previewUri,
            modifier = Modifier.testTag(TEST_TAG_COMPLETED_TRANSFER),
        )
        BottomSheetAction(
            modifier = Modifier.testTag(TEST_TAG_VIEW_IN_FOLDER_ACTION),
            iconId = iconPackR.drawable.ic_file_search02_medium_regular_outline,
            name = stringResource(id = R.string.view_in_folder_label),
            onClick = {
                onViewInFolder()
                onDismissSheet()
            },
        )
        if (type.isDownloadType()) {
            BottomSheetAction(
                modifier = Modifier.testTag(TEST_TAG_OPEN_WITH_ACTION),
                iconId = iconPackR.drawable.ic_external_link_medium_regular_outline,
                name = stringResource(id = R.string.external_play),
                onClick = {
                    onOpenWith()
                    onDismissSheet()
                },
            )
        }
        BottomSheetAction(
            modifier = Modifier.testTag(TEST_TAG_SHARE_LINK_ACTION),
            iconId = iconPackR.drawable.ic_link01_medium_regular_outline,
            name = stringResource(id = R.string.context_get_link),
            onClick = {
                onShareLink()
                onDismissSheet()
            },
        )
        BottomSheetAction(
            modifier = Modifier.testTag(TEST_TAG_CLEAR_ACTION),
            iconId = iconPackR.drawable.ic_eraser_medium_regular_outline,
            name = stringResource(id = R.string.general_clear),
            onClick = {
                onClearTransfer()
                onDismissSheet()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun CompletedTransferActionsBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CompletedTransferActionsBottomSheet(
            completedTransfer = CompletedTransfer(
                id = 0,
                fileName = "2023-03-24 00.13.20_1.pdf",
                type = TransferType.GENERAL_UPLOAD,
                state = TransferState.STATE_COMPLETED,
                size = "3.57 MB",
                handle = 27169983390750L,
                path = "Cloud drive/Camera uploads",
                displayPath = "Cloud drive/Camera uploads",
                isOffline = false,
                timestamp = 1684228012974L,
                error = "No error",
                errorCode = 0,
                originalPath = "/original/path/2023-03-24 00.13.20_1.pdf",
                parentHandle = 11622336899311L,
                appData = emptyList(),
            ),
            fileTypeResId = iconPackR.drawable.ic_pdf_medium_solid,
            previewUri = null,
            onViewInFolder = {},
            onOpenWith = {},
            onShareLink = {},
            onClearTransfer = {},
            onDismissSheet = {},
        )
    }
}

internal const val TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL =
    "$TEST_TAG_COMPLETED_TRANSFERS_VIEW:transfer_actions_panel"
internal const val TEST_TAG_COMPLETED_TRANSFER =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:completed_transfer"
internal const val TEST_TAG_VIEW_IN_FOLDER_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:view_in_folder"
internal const val TEST_TAG_OPEN_WITH_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:open_with"
internal const val TEST_TAG_SHARE_LINK_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:share_link"
internal const val TEST_TAG_CLEAR_ACTION =
    "$TEST_TAG_COMPLETED_TRANSFER_ACTIONS_PANEL:clear"