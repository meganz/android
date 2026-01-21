package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroupImpl
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingDownloadsForNodesUseCase.Companion.MAX_NAMES_TO_SAVE
import javax.inject.Inject

/**
 * Use case to insert pending transfers for upload a list of files to a specific cloud drive folder destination.
 */
class InsertPendingUploadsForFilesUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val timeSystemRepository: TimeSystemRepository,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     * @param pathsAndNames the files to be uploaded with its corresponding name if needs to be changed, if null the file name will be kept
     * @param parentFolderId the id of the folder in the cloud drive where the files will be updated
     * @param isHighPriority whether this uploads are high priority (take precedence over current transfers) or not
     */
    suspend operator fun invoke(
        pathsAndNames: Map<String, String?>,
        parentFolderId: NodeId,
        isHighPriority: Boolean = false,
    ) {
        val location = nodeRepository.getFullNodePathById(parentFolderId)
            ?: nodeRepository.getNodePathById(parentFolderId)
        val pendingTransferNodeId = PendingTransferNodeIdentifier.CloudDriveNode(parentFolderId)
        val transferGroupId = transferRepository.insertActiveTransferGroup(
            ActiveTransferActionGroupImpl(
                transferType = TransferType.GENERAL_UPLOAD,
                destination = location,
                startTime = timeSystemRepository.getCurrentTimeInMillis(),
                pendingTransferNodeId = pendingTransferNodeId,
                selectedNames = pathsAndNames.mapNotNull { it.value }
                    .takeIf { it.isNotEmpty() }?.take(MAX_NAMES_TO_SAVE),
            )
        )
        val appData = listOfNotNull(
            TransferAppData.TransferGroup(transferGroupId),
        )
        transferRepository.insertPendingTransfers(
            pathsAndNames.map { (path, name) ->
                InsertPendingTransferRequest(
                    transferType = TransferType.GENERAL_UPLOAD,
                    nodeIdentifier = pendingTransferNodeId,
                    uriPath = UriPath(path),
                    appData = appData,
                    isHighPriority = isHighPriority,
                    fileName = name,
                    pitagTrigger = PitagTrigger.NotApplicable,
                )
            }
        )
    }
}