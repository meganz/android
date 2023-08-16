package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * [TransferType] to SDK Int mapper
 */
internal class TransferTypeIntMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param transferType [TransferType]
     */
    operator fun invoke(transferType: TransferType) = when (transferType) {
        TransferType.TYPE_DOWNLOAD -> MegaTransfer.TYPE_DOWNLOAD
        TransferType.TYPE_UPLOAD -> MegaTransfer.TYPE_UPLOAD
        TransferType.NONE -> -1
    }
}
