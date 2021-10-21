package mega.privacy.android.app.utils

import mega.privacy.android.app.utils.Constants.APP_DATA_BACKGROUND_TRANSFER
import mega.privacy.android.app.utils.Constants.APP_DATA_VOICE_CLIP
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD

object MegaTransferUtils {

    @JvmStatic
    fun MegaTransfer.isBackgroundTransfer(): Boolean =
        appData?.contains(APP_DATA_BACKGROUND_TRANSFER) == true

    @JvmStatic
    fun MegaTransfer.isVoiceClipType(): Boolean =
        appData?.contains(APP_DATA_VOICE_CLIP) == true

    @JvmStatic
    fun ArrayList<MegaTransfer>.getSilentNumPendingDownloads(): Int {
        var count = 0
        forEach { transfer ->
            if (transfer.type == TYPE_DOWNLOAD && !transfer.isBackgroundTransfer()) count++
        }
        return count
    }
}
