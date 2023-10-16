package mega.privacy.android.data.mapper.offline

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.OfflineEntity
import mega.privacy.android.domain.entity.Offline
import javax.inject.Inject

internal class OfflineModelMapper @Inject constructor(
    private val decryptData: DecryptData,
) {
    suspend operator fun invoke(offlineEntity: OfflineEntity) = Offline(
        id = offlineEntity.id ?: -1,
        handle = decryptData(offlineEntity.encryptedHandle).orEmpty(),
        path = decryptData(offlineEntity.encryptedPath).orEmpty(),
        name = decryptData(offlineEntity.encryptedName).orEmpty(),
        parentId = offlineEntity.parentId ?: -1,
        type = decryptData(offlineEntity.encryptedType).orEmpty(),
        origin = offlineEntity.incoming ?: -1,
        handleIncoming = decryptData(offlineEntity.encryptedIncomingHandle).orEmpty(),
        lastModifiedTime = offlineEntity.lastModifiedTime ?: 0
    )
}