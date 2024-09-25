package mega.privacy.android.data.mapper.transfer.pending

import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import javax.inject.Inject

/**
 * Mapper for [PendingTransfer] to its database entity [PendingTransferEntity]
 */
internal class PendingTransferEntityMapper @Inject constructor(
    private val transferAppDataStringMapper: TransferAppDataStringMapper,
) {
    operator fun invoke(pendingTransfer: PendingTransfer) = with(pendingTransfer) {
        PendingTransferEntity(
            pendingTransferId = pendingTransferId,
            transferTag = transferTag,
            transferType = transferType,
            nodeIdentifier = nodeIdentifier,
            path = path,
            appData = transferAppDataStringMapper(listOfNotNull(appData)),
            isHighPriority = isHighPriority,
            scanningFoldersData = with(scanningFoldersData) {
                PendingTransferEntity.ScanningFoldersDataEntity(
                    stage = stage,
                    fileCount = fileCount,
                    folderCount = folderCount,
                    createdFolderCount = createdFolderCount,
                )
            },
            startedFiles = startedFiles,
            alreadyTransferred = alreadyTransferred,
            state = state,
        )
    }
}