package mega.privacy.android.data.mapper.transfer.completed

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import javax.inject.Inject

internal class CompletedTransferModelMapper @Inject constructor(
    private val decryptData: DecryptData,
) {
    suspend operator fun invoke(entity: CompletedTransferEntity) = CompletedTransfer(
        id = entity.id,
        fileName = decryptData(entity.fileName).orEmpty(),
        type = decryptData(entity.type)?.toInt() ?: -1,
        state = decryptData(entity.state)?.toInt() ?: -1,
        size = decryptData(entity.size).orEmpty(),
        handle = decryptData(entity.handle)?.toLong() ?: -1L,
        path = decryptData(entity.path).orEmpty(),
        isOffline = decryptData(entity.isOffline)?.toBooleanStrictOrNull(),
        timestamp = decryptData(entity.timestamp)?.toLong() ?: -1L,
        error = decryptData(entity.error),
        originalPath = decryptData(entity.originalPath).orEmpty(),
        parentHandle = decryptData(entity.parentHandle)?.toLong() ?: -1L,
    )
}
