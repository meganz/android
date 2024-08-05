package mega.privacy.android.app.presentation.transfers.view.dialog

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ProgressDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.MinimumTimeVisibility

/**
 * [ProgressDialog] for showing scanning transfers progress information. It also allows to cancel the transfers and shows a [ProgressDialog] while cancelling.
 * @param startTransferJobInProgress the current progress of the start transfer job to be shown
 * @param modifier
 * @param onCancel callback that will be invoked when the user taps "Cancel transfers" button
 */
@Composable
fun TransferInProgressDialog(
    startTransferJobInProgress: StartTransferJobInProgress?,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
) {
    if (startTransferJobInProgress is StartTransferJobInProgress.CancellingTransfers) {
        ProgressDialog(
            title = stringResource(id = sharedR.string.transfers_scanning_folders_dialog_cancel_cancelling_title),
            progress = null,
        )
    } else {
        MinimumTimeVisibility(visible = startTransferJobInProgress != null) {
            TransferInProgressScanningDialog(
                scanningTransfersProgress = (startTransferJobInProgress as? StartTransferJobInProgress.ScanningTransfers),
                modifier = modifier,
                onCancel = onCancel
            )
        }
    }
}

@Composable
internal fun TransferInProgressScanningDialog(
    scanningTransfersProgress: StartTransferJobInProgress.ScanningTransfers?,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
) {
    val title = stringResource(
        when (scanningTransfersProgress?.stage) {
            null, TransferStage.STAGE_NONE, TransferStage.STAGE_SCANNING -> R.string.scanning_transfers
            TransferStage.STAGE_CREATING_TREE -> if (scanningTransfersProgress.allFoldersCreated()) {
                sharedR.string.transfers_scanning_folders_dialog_starting_transfers_title
            } else {
                sharedR.string.transfers_scanning_folders_dialog_creating_folders_title
            }

            TransferStage.STAGE_TRANSFERRING_FILES -> sharedR.string.transfers_scanning_folders_dialog_starting_transfers_title
        }
    )
    val subtitle =
        when (scanningTransfersProgress?.stage) {
            null, TransferStage.STAGE_TRANSFERRING_FILES, TransferStage.STAGE_NONE -> null
            TransferStage.STAGE_SCANNING -> {
                pluralStringResource(
                    id = sharedR.plurals.transfers_scanning_folders_dialog_scanning_files_and_folders_subtitle,
                    count = scanningTransfersProgress.fileCount,
                    pluralStringResource(
                        id = sharedR.plurals.transfers_scanning_folders_dialog_scanning_folders_subtitle,
                        count = scanningTransfersProgress.folderCount,
                        scanningTransfersProgress.folderCount,
                    ),
                    scanningTransfersProgress.fileCount,
                )
            }

            TransferStage.STAGE_CREATING_TREE -> if (scanningTransfersProgress.allFoldersCreated()) {
                null
            } else {
                pluralStringResource(
                    id = sharedR.plurals.transfers_scanning_folders_dialog_creating_folders_subtitle,
                    count = scanningTransfersProgress.createdFolderCount,
                    scanningTransfersProgress.createdFolderCount,
                    scanningTransfersProgress.folderCount
                )
            }
        }
    val info = stringResource(sharedR.string.transfers_scanning_folders_dialog_cancel_info_subtitle)
    val subtitleWithInfo = listOfNotNull(subtitle, info).joinToString("\n")
    ProgressDialog(
        title = title,
        subTitle = subtitleWithInfo,
        progress = null,
        cancelButtonText = stringResource(R.string.cancel_transfers),
        onCancel = onCancel,
        modifier = modifier.testTag(PROGRESS_TAG),
    )
}


@CombinedThemePreviews
@Composable
private fun TransferInProgressDialogPreview(@PreviewParameter(DeviceBottomSheetBodyPreviewProvider::class) scanningTransfersProgress: StartTransferJobInProgress.ScanningTransfers) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TransferInProgressDialog(scanningTransfersProgress) {}
    }
}

private class DeviceBottomSheetBodyPreviewProvider :
    PreviewParameterProvider<StartTransferJobInProgress?> {
    override val values = sequenceOf(
        null,
        StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_NONE),
        StartTransferJobInProgress.ScanningTransfers(
            TransferStage.STAGE_SCANNING,
            fileCount = 1,
            folderCount = 1
        ),
        StartTransferJobInProgress.ScanningTransfers(
            TransferStage.STAGE_SCANNING,
            fileCount = 16,
            folderCount = 1
        ),
        StartTransferJobInProgress.ScanningTransfers(
            TransferStage.STAGE_SCANNING,
            fileCount = 14364,
            folderCount = 156
        ),
        StartTransferJobInProgress.ScanningTransfers(
            TransferStage.STAGE_CREATING_TREE,
            fileCount = 1,
            folderCount = 5,
            createdFolderCount = 1
        ),
        StartTransferJobInProgress.ScanningTransfers(
            TransferStage.STAGE_CREATING_TREE,
            fileCount = 1,
            folderCount = 5,
            createdFolderCount = 5
        ),
        StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_TRANSFERRING_FILES),
        StartTransferJobInProgress.CancellingTransfers,
    )
}

internal const val PROGRESS_TAG = "transfer_in_progress_dialog:dialog_progress"