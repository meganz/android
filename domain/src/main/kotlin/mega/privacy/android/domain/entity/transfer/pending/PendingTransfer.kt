package mega.privacy.android.domain.entity.transfer.pending

import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer.ScanningFoldersData

/**
 * Pending transfer. The user has selected this transfer but it's still not sent to the SDK or it's waiting for SDK folder scanning process
 * @property pendingTransferId
 * @property transferTag Sdk transfer tag
 * @property transferType
 * @property nodeIdentifier the identifier to fetch the related node. The node to download or the folder node to upload to.
 * @property path the path where this node will be downloaded or uploaded
 * @property appData the [TransferAppData] associated to this transfer
 * @property isHighPriority
 * @property scanningFoldersData [ScanningFoldersData] of this transfer if the SDK is in the scanning folders process
 * @property startedFiles
 * @property alreadyTransferred
 * @property state the state of this pending transfer
 */
data class PendingTransfer(
    val pendingTransferId: Long,
    val transferTag: Int? = null,
    val transferType: TransferType,
    val nodeIdentifier: PendingTransferNodeIdentifier,
    val path: String,
    val appData: TransferAppData?,
    val isHighPriority: Boolean,
    val scanningFoldersData: ScanningFoldersData = ScanningFoldersData(),
    val startedFiles: Int = 0,
    val alreadyTransferred: Int = 0,
    val state: PendingTransferState = PendingTransferState.NotSentToSdk,
) {

    /**
     * Pending transfer data related to scanning folders process
     * @property stage the stage of this transfer scanning process
     * @property fileCount the number of files scanned
     * @property folderCount the number of folders scanned
     * @property createdFolderCount the number of folders already created
     */
    data class ScanningFoldersData(
        val stage: TransferStage = TransferStage.STAGE_NONE,
        val fileCount: Int = 0,
        val folderCount: Int = 0,
        val createdFolderCount: Int = 0,
    )

    /**
     * If true, this Pending transfer has finished and is not needed anymore, either because the transfer has already started or failed.
     */
    fun finished() = state in listOf(
        PendingTransferState.AlreadyStarted,
        PendingTransferState.ErrorStarting,
    )

    /**
     * If true, this pending transfer is still needed to start or monitor the start of the transfer.
     */
    fun notFinished() = !finished()
}
