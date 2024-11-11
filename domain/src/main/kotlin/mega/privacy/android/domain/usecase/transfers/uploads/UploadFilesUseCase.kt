package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.UploadFileInfo
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
) : AbstractTransferNodesUseCase<UploadFileInfo, String>(
    cancelCancelTokenUseCase,
    invalidateCancelTokenUseCase,
    handleTransferEventUseCase,
    monitorTransferEventsUseCase,
) {

    /**
     * Invoke
     *
     * @param uploadFileInfos files and / or folders with the data needed to do the upload
     * @param parentFolderId destination folder id where [uploadFileInfos] will be uploaded
     * @param isHighPriority Whether the file or folder should be placed on top of the upload queue or not, chat uploads are always priority regardless of this parameter
     *
     * @return a flow of [MultiTransferEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        uploadFileInfos: List<UploadFileInfo>,
        parentFolderId: NodeId,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> {
        return super.commonInvoke(
            items = uploadFileInfos,
            null,
        ) { uploadFileInfo ->
            val isSourceTemporary =
                cacheRepository.isFileInCacheDirectory(File(uploadFileInfo.uriPath.value))
            if (!uploadFileInfo.appData.isNullOrEmpty() && uploadFileInfo.appData.all { it is TransferAppData.ChatTransferAppData }) {
                @Suppress("UNCHECKED_CAST")
                transferRepository.startUploadForChat(
                    localPath = uploadFileInfo.uriPath.value,
                    parentNodeId = parentFolderId,
                    fileName = uploadFileInfo.fileName,
                    appData = uploadFileInfo.appData as List<TransferAppData.ChatTransferAppData>,
                    isSourceTemporary = isSourceTemporary,
                )
            } else {
                transferRepository.startUpload(
                    localPath = uploadFileInfo.uriPath.value,
                    parentNodeId = parentFolderId,
                    fileName = uploadFileInfo.fileName,
                    modificationTime = null,
                    appData = uploadFileInfo.appData,
                    isSourceTemporary = isSourceTemporary,
                    shouldStartFirst = isHighPriority,
                )
            }
        }
    }

    override fun generateIdFromItem(item: UploadFileInfo): String =
        item.uriPath.value

    override fun generateIdFromTransferEvent(transferEvent: TransferEvent) =
        transferEvent.transfer.localPath

}