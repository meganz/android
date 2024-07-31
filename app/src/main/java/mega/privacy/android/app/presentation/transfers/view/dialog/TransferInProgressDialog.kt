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
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ProgressDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * [ProgressDialog] for showing transfer processing in progress, it handles cancel confirmation showing a [ConfirmationDialog]
 */
@Composable
fun TransferInProgressDialog(
    scanningTransfersProgress: StartTransferJobInProgress.ScanningTransfers?,
    modifier: Modifier = Modifier,
    onCancelConfirmed: () -> Unit,
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
                if (scanningTransfersProgress.folderCount == 1) {
                    pluralStringResource(
                        id = sharedR.plurals.transfers_scanning_folders_dialog_scanning_folder_subtitle,
                        count = scanningTransfersProgress.fileCount,
                        scanningTransfersProgress.fileCount,
                    )
                } else {
                    pluralStringResource(
                        id = sharedR.plurals.transfers_scanning_folders_dialog_scanning_folders_subtitle,
                        count = scanningTransfersProgress.fileCount,
                        scanningTransfersProgress.folderCount,
                        scanningTransfersProgress.fileCount,
                    )
                }
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
        onCancel = onCancelConfirmed,
        modifier = modifier.testTag(PROGRESS_TAG),
    )
}


@CombinedThemePreviews
@Composable
private fun PreviewTransferInProgressDialog(@PreviewParameter(DeviceBottomSheetBodyPreviewProvider::class) scanningTransfersProgress: StartTransferJobInProgress.ScanningTransfers) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TransferInProgressDialog(scanningTransfersProgress) {}
    }
}

private class DeviceBottomSheetBodyPreviewProvider :
    PreviewParameterProvider<StartTransferJobInProgress.ScanningTransfers?> {
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
            fileCount = 2,
            folderCount = 2
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
    )
}

internal const val PROGRESS_TAG = "transfer_in_progress_dialog:dialog_progress"