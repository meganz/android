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
import mega.privacy.android.app.presentation.transfers.view.failed.TEST_TAG_FAILED_TRANSFERS_VIEW
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.feature.shared.components.BottomSheetAction
import mega.privacy.android.feature.transfers.components.FailedTransferBottomSheetHeader
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Bottom sheet for a failed transfer actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FailedTransferActionsBottomSheet(
    failedTransfer: CompletedTransfer,
    fileTypeResId: Int?,
    previewUri: Uri?,
    onRetryTransfer: () -> Unit,
    onClearTransfer: () -> Unit,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) = with(failedTransfer) {
    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        onDismissRequest = onDismissSheet,
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(TEST_TAG_FAILED_TRANSFER_ACTIONS_PANEL)
            .fillMaxWidth(),
        sheetState = sheetState,
    ) {
        FailedTransferBottomSheetHeader(
            fileName = fileName,
            info = if (isError) {
                String.format(
                    "%s: %s",
                    stringResource(R.string.failed_label),
                    error
                )
            } else {
                stringResource(R.string.transfer_cancelled)
            },
            isError = isError,
            fileTypeResId = fileTypeResId,
            previewUri = previewUri,
            modifier = Modifier.testTag(TEST_TAG_FAILED_TRANSFER),
        )
        BottomSheetAction(
            modifier = Modifier.testTag(TEST_TAG_RETRY_ACTION),
            iconPainter = IconPack.Medium.Thin.Outline.RotateCcw,
            name = stringResource(id = R.string.general_retry),
            onClick = {
                onRetryTransfer()
                onDismissSheet()
            },
        )
        BottomSheetAction(
            modifier = Modifier.testTag(TEST_TAG_CLEAR_FAILED_TRANSFER_ACTION),
            iconPainter = IconPack.Medium.Thin.Outline.Eraser,
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
private fun FailedTransferActionsBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FailedTransferActionsBottomSheet(
            failedTransfer = CompletedTransfer(
                id = 0,
                fileName = "2023-03-24 00.13.20_1.pdf",
                type = TransferType.GENERAL_UPLOAD,
                state = TransferState.STATE_FAILED,
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
            onRetryTransfer = {},
            onClearTransfer = {},
            onDismissSheet = {},
        )
    }
}

internal const val TEST_TAG_FAILED_TRANSFER_ACTIONS_PANEL =
    "$TEST_TAG_FAILED_TRANSFERS_VIEW:transfer_actions_panel"
internal const val TEST_TAG_FAILED_TRANSFER =
    "$TEST_TAG_FAILED_TRANSFER_ACTIONS_PANEL:failed_transfer"
internal const val TEST_TAG_RETRY_ACTION =
    "$TEST_TAG_FAILED_TRANSFER_ACTIONS_PANEL:retry"
internal const val TEST_TAG_CLEAR_FAILED_TRANSFER_ACTION =
    "$TEST_TAG_FAILED_TRANSFER_ACTIONS_PANEL:clear"