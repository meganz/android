package mega.privacy.android.data.mapper.transfer.sd

import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.domain.entity.SdTransfer
import javax.inject.Inject

internal class SdTransferEntityMapper @Inject constructor(
    private val encryptData: EncryptData,
) {
    suspend operator fun invoke(sdTransfer: SdTransfer) = SdTransferEntity(
        tag = sdTransfer.tag,
        encryptedHandle = encryptData(sdTransfer.nodeHandle),
        encryptedName = encryptData(sdTransfer.name),
        encryptedSize = encryptData(sdTransfer.size),
        encryptedPath = encryptData(sdTransfer.path),
        encryptedAppData = encryptData(sdTransfer.appData),
    )
}