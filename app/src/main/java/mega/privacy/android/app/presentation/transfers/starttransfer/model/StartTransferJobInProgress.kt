package mega.privacy.android.app.presentation.transfers.starttransfer.model

import mega.privacy.android.domain.entity.transfer.TransferStage

/**
 * Represents the job in progress state
 */
sealed interface StartTransferJobInProgress {

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
    ) : StartTransferJobInProgress {
        /**
         * @return true if there are folders and all the folders has been created
         */
        fun allFoldersCreated() = folderCount > 0 && folderCount == createdFolderCount
    }

    /**
     * User cancelled the transfers while the Scanning transfers are processed and waiting for Sdk to actually cancel them
     */
    data object CancellingTransfers : StartTransferJobInProgress
}