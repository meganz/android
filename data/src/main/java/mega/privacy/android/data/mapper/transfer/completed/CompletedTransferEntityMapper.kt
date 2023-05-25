package mega.privacy.android.data.mapper.transfer.completed

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

internal class CompletedTransferEntityMapper @Inject constructor(
    private val encryptData: EncryptData,
) {
    suspend operator fun invoke(completedTransfer: CompletedTransfer) = CompletedTransferEntity(
        fileName = encryptData(completedTransfer.fileName),
        type = encryptData(completedTransfer.type.toString()),
        state = encryptData(completedTransfer.state.toString()),
        size = encryptData(completedTransfer.size),
        handle = encryptData(completedTransfer.handle.toString()),
        path = encryptData(completedTransfer.path),
        isOffline = encryptData(completedTransfer.isOffline.toString()),
        timestamp = encryptData(completedTransfer.timestamp.toString()),
        error = encryptData(completedTransfer.error),
        originalPath = encryptData(completedTransfer.originalPath),
        parentHandle = encryptData(completedTransfer.parentHandle.toString()),
    )
}
