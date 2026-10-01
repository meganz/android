package mega.privacy.android.domain.entity.transfer.pending

import mega.privacy.android.domain.entity.transfer.TransferStage

/**
 * Update pending transfer request interface
 * @property pendingTransferId
 */
sealed interface UpdatePendingTransferRequest {
    val pendingTransferId: Long
}

/**
 * Update pending transfer state
 * @property state
 */
data class UpdatePendingTransferState(
    override val pendingTransferId: Long,
    val state: PendingTransferState,
) : UpdatePendingTransferRequest

/**
 * Update pending transfer started and already transferred files
 * @property startedFiles
 * @property alreadyTransferred
 * @property state
 */
data class UpdateAlreadyTransferredFilesCount(
    override val pendingTransferId: Long,
    val startedFiles: Int,
    val alreadyTransferred: Int,
    val state: PendingTransferState = PendingTransferState.AlreadyStarted,
) : UpdatePendingTransferRequest

/**
 * Update Scanning folders data
 * @property stage
 * @property fileCount
 * @property folderCount
 * @property createdFolderCount
 */
data class UpdateScanningFoldersData(
    override val pendingTransferId: Long,
    val stage: TransferStage = TransferStage.STAGE_NONE,
    val fileCount: Int = 0,
    val folderCount: Int = 0,
    val createdFolderCount: Int = 0,
) : UpdatePendingTransferRequest