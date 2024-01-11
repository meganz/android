package mega.privacy.android.domain.usecase.transfers.uploads

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use Case to upload a file or folder
 */
class StartUploadUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invokes the Use Case to upload a file or folder
     *
     * @param localPath The local path of the file or folder
     * @param parentNodeId The parent node for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * @param modificationTime The custom modification time for the file or folder, denoted in
     * seconds since the epoch
     * @param appData The custom app data to save, which can be nullable
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload
     * should be deleted or not
     * @param shouldStartFirst Whether the file or folder should be placed on top of the upload
     * queue or not
     *
     * @return A flow of [TransferEvent]
     */
    operator fun invoke(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        modificationTime: Long,
        appData: TransferAppData?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
    ): Flow<TransferEvent> =
        transferRepository.startUpload(
            localPath = localPath,
            parentNodeId = parentNodeId,
            fileName = fileName,
            modificationTime = modificationTime,
            appData = appData,
            isSourceTemporary = isSourceTemporary,
            shouldStartFirst = shouldStartFirst,
        )
}
