package mega.privacy.android.data.mapper.transfer.pending

import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import javax.inject.Inject

internal class InsertPendingTransferRequestMapper @Inject constructor(
    private val transferAppDataStringMapper: TransferAppDataStringMapper,
) {
    operator fun invoke(pendingTransfer: InsertPendingTransferRequest) = with(pendingTransfer) {
        PendingTransferEntity(
            transferType = transferType,
            nodeIdentifier = nodeIdentifier,
            transferTag = null,
            path = path,
            appData = transferAppDataStringMapper(listOfNotNull(appData)),
            isHighPriority = isHighPriority,
            scanningFoldersData = PendingTransferEntity.ScanningFoldersDataEntity(),
            startedFiles = 0,
            alreadyTransferred = 0,
            state = PendingTransferState.NotSentToSdk,
        )
    }
}