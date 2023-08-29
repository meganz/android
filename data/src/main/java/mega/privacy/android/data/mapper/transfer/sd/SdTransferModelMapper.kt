package mega.privacy.android.data.mapper.transfer.sd

import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.domain.entity.SdTransfer
import javax.inject.Inject

internal class SdTransferModelMapper @Inject constructor(
    private val decryptData: DecryptData,
) {
    suspend operator fun invoke(entity: SdTransferEntity) = SdTransfer(
        tag = entity.tag ?: 0,
        nodeHandle = decryptData(entity.encryptedHandle).orEmpty(),
        name = decryptData(entity.encryptedName).orEmpty(),
        size = decryptData(entity.encryptedSize).orEmpty(),
        path = decryptData(entity.encryptedPath).orEmpty(),
        appData = decryptData(entity.encryptedAppData).orEmpty(),
    )
}