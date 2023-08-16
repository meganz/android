package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferType
import nz.mega.sdk.MegaTransfer
import javax.inject.Inject

/**
 * [TransferType] mapper
 */
internal class TransferTypeMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param transferInt
     */
    operator fun invoke(transferInt: Int) = when (transferInt) {
        MegaTransfer.TYPE_DOWNLOAD -> TransferType.TYPE_DOWNLOAD
        MegaTransfer.TYPE_UPLOAD -> TransferType.TYPE_UPLOAD
        else -> TransferType.NONE
    }
}
