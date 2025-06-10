package mega.privacy.android.data.mapper.transfer.completed

import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.mapper.transfer.TransferAppDataStringMapper
import mega.privacy.android.data.mapper.transfer.TransferStateIntMapper
import mega.privacy.android.data.mapper.transfer.TransferTypeIntMapper
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

internal class CompletedTransferEntityMapper @Inject constructor(
    private val transferTypeMapper: TransferTypeIntMapper,
    private val transferStateMapper: TransferStateIntMapper,
    private val transferAppDataMapper: TransferAppDataStringMapper,
) {
    suspend operator fun invoke(completedTransfer: CompletedTransfer) = CompletedTransferEntity(
        id = completedTransfer.id?.takeIf { it > 0 },
        fileName = completedTransfer.fileName,
        type = transferTypeMapper(completedTransfer.type),
        state = transferStateMapper(completedTransfer.state),
        size = completedTransfer.size,
        handle = completedTransfer.handle,
        path = completedTransfer.path,
        displayPath = completedTransfer.displayPath,
        isOffline = completedTransfer.isOffline,
        timestamp = completedTransfer.timestamp,
        error = completedTransfer.error,
        errorCode = completedTransfer.errorCode,
        originalPath = completedTransfer.originalPath,
        parentHandle = completedTransfer.parentHandle,
        appData = transferAppDataMapper(completedTransfer.appData),
    )
}
