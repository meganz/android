package mega.privacy.android.data.mapper.offline

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.OfflineEntity
import mega.privacy.android.domain.entity.Offline
import javax.inject.Inject

internal class OfflineEntityMapper @Inject constructor(
    private val encryptData: EncryptData,
) {
    suspend operator fun invoke(offline: Offline) = OfflineEntity(
        encryptedHandle = encryptData(offline.handle),
        encryptedPath = encryptData(offline.path),
        encryptedName = encryptData(offline.name),
        parentId = offline.parentId,
        encryptedType = encryptData(offline.type),
        incoming = offline.origin,
        encryptedIncomingHandle = encryptData(offline.handleIncoming),
        lastModifiedTime = offline.lastModifiedTime
    )
}