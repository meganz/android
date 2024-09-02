package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractTransferNodesUseCase
import java.io.File
import javax.inject.Inject

/**
 * Uploads a list of files to the specified destination folder and returns a Flow to monitor the progress
 */
class UploadFilesUseCase @Inject constructor(
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    handleTransferEventUseCase: HandleTransferEventUseCase,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val transferRepository: TransferRepository,
    private val cacheRepository: CacheRepository,
) : AbstractTransferNodesUseCase<File, String>(
    cancelCancelTokenUseCase,
    invalidateCancelTokenUseCase,
    handleTransferEventUseCase,
    monitorTransferEventsUseCase,
) {

    /**
     * Invoke
     *
     * @param filesAndNames files and / or folders to be uploaded, associated with the desired node's name or null if there are no changes
     * @param parentFolderId destination folder id where [filesAndNames] will be uploaded
     * @param appData Custom app data to save in the MegaTransfer object.
     * @param isHighPriority Whether the file or folder should be placed on top of the upload queue or not, chat uploads are always priority regardless of this parameter
     *
     * @return a flow of [MultiTransferEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        filesAndNames: Map<File, String?>,
        parentFolderId: NodeId,
        appData: List<TransferAppData>?,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> {
        return super.commonInvoke(
            items = filesAndNames.keys.toList(),
            null,
        ) { file ->
            val isSourceTemporary = cacheRepository.isFileInCacheDirectory(file)
            if (!appData.isNullOrEmpty() && appData.all { it is TransferAppData.ChatTransferAppData }) {
                @Suppress("UNCHECKED_CAST")
                transferRepository.startUploadForChat(
                    localPath = file.absolutePath,
                    parentNodeId = parentFolderId,
                    fileName = filesAndNames[file],
                    appData = appData as List<TransferAppData.ChatTransferAppData>,
                    isSourceTemporary = isSourceTemporary,
                )
            } else {
                transferRepository.startUpload(
                    localPath = file.absolutePath,
                    parentNodeId = parentFolderId,
                    fileName = filesAndNames[file],
                    modificationTime = file.lastModified() / 1000,
                    appData = appData,
                    isSourceTemporary = isSourceTemporary,
                    shouldStartFirst = isHighPriority,
                )
            }
        }
    }

    override fun generateIdFromItem(item: File): String =
        item.path

    override fun generateIdFromTransferEvent(transferEvent: TransferEvent) =
        transferEvent.transfer.localPath

}