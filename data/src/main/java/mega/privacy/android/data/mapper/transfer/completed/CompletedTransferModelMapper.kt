package mega.privacy.android.data.mapper.transfer.completed

import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

internal class CompletedTransferModelMapper @Inject constructor() {
    suspend operator fun invoke(entity: CompletedTransferEntity) = CompletedTransfer(
        id = entity.id,
        fileName = entity.fileName,
        type = entity.type,
        state = entity.state,
        size = entity.size,
        handle = entity.handle,
        path = entity.path,
        isOffline = entity.isOffline,
        timestamp = entity.timestamp,
        error = entity.error,
        originalPath = entity.originalPath,
        parentHandle = entity.parentHandle,
        appData = entity.appData,
    )
}
