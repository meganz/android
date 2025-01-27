package mega.privacy.android.data.mapper.transfer.pending

import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataMapper
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.uri.UriPath
import javax.inject.Inject

/**
 * Mapper for [PendingTransferEntity] to its model class [PendingTransfer]
 */
internal class PendingTransferModelMapper @Inject constructor(
    private val appDataMapper: TransferAppDataMapper,
) {
    operator fun invoke(pendingTransfer: PendingTransferEntity) = with(pendingTransfer) {
        PendingTransfer(
            pendingTransferId = pendingTransferId ?: -1,
            transferTag = transferTag,
            transferType = transferType,
            nodeIdentifier = nodeIdentifier,
            uriPath = UriPath(path),
            appData = appData?.let { appDataMapper(it) },
            isHighPriority = isHighPriority,
            scanningFoldersData = with(scanningFoldersData) {
                PendingTransfer.ScanningFoldersData(
                    stage = stage,
                    fileCount = fileCount,
                    folderCount = folderCount,
                    createdFolderCount = createdFolderCount,
                )
            },
            startedFiles = startedFiles,
            alreadyTransferred = alreadyTransferred,
            state = state,
            fileName = fileName,
        )
    }
}