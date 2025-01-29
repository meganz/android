package mega.privacy.android.domain.usecase.transfers.pending

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to insert pending transfers for upload a list of files to a specific cloud drive folder destination.
 */
class InsertPendingUploadsForFilesUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     * @param pathsAndNames the files to be uploaded with its corresponding name if needs to be changed, if null the file name will be kept
     * @param appData
     * @param parentFolderId the id of the folder in the cloud drive where the files will be updated
     * @param isHighPriority whether this uploads are high priority (take precedence over current transfers) or not
     */
    suspend operator fun invoke(
        pathsAndNames: Map<String, String?>,
        parentFolderId: NodeId,
        isHighPriority: Boolean = false,
        appData: List<TransferAppData>? = null,
    ) {
        transferRepository.insertPendingTransfers(
            pathsAndNames.map { (path, name) ->
                InsertPendingTransferRequest(
                    transferType = TransferType.GENERAL_UPLOAD,
                    nodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(parentFolderId),
                    uriPath = UriPath(path),
                    appData = appData,
                    isHighPriority = isHighPriority,
                    fileName = name
                )
            }
        )
    }
}