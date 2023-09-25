package mega.privacy.android.data.mapper.transfer

import mega.privacy.android.domain.entity.transfer.TransferAppData
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
    operator fun invoke(transferInt: Int, appData: List<TransferAppData>) = when (transferInt) {
        MegaTransfer.TYPE_DOWNLOAD -> TransferType.DOWNLOAD
        MegaTransfer.TYPE_UPLOAD -> when {
            appData.any { it is TransferAppData.ChatUpload } -> TransferType.CHAT_UPLOAD
            appData.contains(TransferAppData.CameraUpload) -> TransferType.CAMERA_UPLOAD
            else -> TransferType.GENERAL_UPLOAD
        }

        else -> TransferType.NONE
    }
}
