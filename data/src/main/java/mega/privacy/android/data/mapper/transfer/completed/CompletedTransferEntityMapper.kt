package mega.privacy.android.data.mapper.transfer.completed

import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

internal class CompletedTransferEntityMapper @Inject constructor() {
    suspend operator fun invoke(completedTransfer: CompletedTransfer) = CompletedTransferEntity(
        id = completedTransfer.id?.takeIf { it > 0 },
        fileName = completedTransfer.fileName,
        type = completedTransfer.type,
        state = completedTransfer.state,
        size = completedTransfer.size,
        handle = completedTransfer.handle,
        path = completedTransfer.path,
        isOffline = completedTransfer.isOffline,
        timestamp = completedTransfer.timestamp,
        error = completedTransfer.error,
        originalPath = completedTransfer.originalPath,
        parentHandle = completedTransfer.parentHandle,
        appData = completedTransfer.appData,
    )
}
