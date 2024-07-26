package mega.privacy.android.app.presentation.transfers.starttransfer.model

import mega.privacy.android.domain.entity.transfer.TransferStage

/**
 * Represents the job in progress state
 */
sealed class StartTransferJobInProgress {

    /**
     * Sdk is scanning the transfers
     * @property stage The current transfer stage
     * @property fileCount The total file count to scan
     * @property folderCount The total folder count to scan
     * @property createdFolderCount The number of folders already created
     */
    data class ScanningTransfers(
        val stage: TransferStage,
        val fileCount: Int = 0,
        val folderCount: Int = 0,
        val createdFolderCount: Int = 0,
    ) : StartTransferJobInProgress()
}