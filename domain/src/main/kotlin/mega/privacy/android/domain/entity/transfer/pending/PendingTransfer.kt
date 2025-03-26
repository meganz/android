package mega.privacy.android.domain.entity.transfer.pending

import mega.privacy.android.domain.entity.transfer.AppDataOwner
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer.ScanningFoldersData
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Pending transfer. The user has selected this transfer but it's still not sent to the SDK or it's waiting for SDK folder scanning process
 * @property pendingTransferId
 * @property transferUniqueId Sdk transfer uniqueId
 * @property transferType
 * @property nodeIdentifier the identifier to fetch the related node. The node to download or the folder node to upload to.
 * @property uriPath the [UriPath] where this node will be downloaded to or uploaded from
 * @property appData the [TransferAppData] associated to this transfer
 * @property isHighPriority
 * @property scanningFoldersData [ScanningFoldersData] of this transfer if the SDK is in the scanning folders process
 * @property startedFiles
 * @property alreadyTransferred
 * @property state the state of this pending transfer
 * @property fileName The name of the file to be shown in completed Transfers. It can be used to rename uploaded nodes. Current file name will be used if not specified.
 */
data class PendingTransfer(
    val pendingTransferId: Long,
    val transferUniqueId: Long? = null,
    val transferType: TransferType,
    val nodeIdentifier: PendingTransferNodeIdentifier,
    val uriPath: UriPath,
    override val appData: List<TransferAppData> = emptyList(),
    val isHighPriority: Boolean,
    val scanningFoldersData: ScanningFoldersData = ScanningFoldersData(),
    val startedFiles: Int = 0,
    val alreadyTransferred: Int = 0,
    val state: PendingTransferState = PendingTransferState.NotSentToSdk,
    val fileName: String?,
) : AppDataOwner {

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
     * If true, this Pending transfer is not needed anymore, either because the transfer has already started, scanned or failed.
     */
    fun resolved() = state in listOf(
        PendingTransferState.SdkScanned,
        PendingTransferState.AlreadyStarted,
        PendingTransferState.ErrorStarting,
    )
}
