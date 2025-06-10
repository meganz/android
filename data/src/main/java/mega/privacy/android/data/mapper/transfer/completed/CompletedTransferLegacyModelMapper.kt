package mega.privacy.android.data.mapper.transfer.completed

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntityLegacy
import javax.inject.Inject

internal class CompletedTransferLegacyModelMapper @Inject constructor(
    private val decryptData: DecryptData,
    private val completedTransferModelMapper: CompletedTransferModelMapper,
) {
    suspend operator fun invoke(entity: CompletedTransferEntityLegacy) =
        completedTransferModelMapper(
            CompletedTransferEntity(
                id = entity.id,
                fileName = decryptData(entity.fileName).orEmpty(),
                type = decryptData(entity.type)?.toIntOrNull() ?: -1,
                state = decryptData(entity.state)?.toIntOrNull() ?: -1,
                size = decryptData(entity.size).orEmpty(),
                handle = decryptData(entity.handle)?.toLongOrNull() ?: -1L,
                path = decryptData(entity.path).orEmpty(),
                displayPath = null,
                isOffline = decryptData(entity.isOffline)?.toBooleanStrictOrNull(),
                timestamp = decryptData(entity.timestamp)?.toLongOrNull() ?: -1L,
                error = decryptData(entity.error),
                errorCode = null,
                originalPath = decryptData(entity.originalPath).orEmpty(),
                parentHandle = decryptData(entity.parentHandle)?.toLongOrNull() ?: -1L,
                appData = decryptData(entity.appData),
            )
        )
}
