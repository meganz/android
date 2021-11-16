package mega.privacy.android.app.utils

import mega.privacy.android.app.utils.Constants.APP_DATA_BACKGROUND_TRANSFER
import mega.privacy.android.app.utils.Constants.APP_DATA_VOICE_CLIP
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD

object MegaTransferUtils {

    /**
     * Kotlin extension function to check whether a MegaTransfer is a background transfer.
     *
     * @return  True if it is, false otherwise.
     */
    @JvmStatic
    fun MegaTransfer.isBackgroundTransfer(): Boolean =
        appData?.contains(APP_DATA_BACKGROUND_TRANSFER) == true

    /**
     * Kotlin extension function to check whether a MegaTransfer is a voice clip type transfer.
     *
     * @return  True if it is, false otherwise.
     */
    @JvmStatic
    fun MegaTransfer.isVoiceClipType(): Boolean =
        appData?.contains(APP_DATA_VOICE_CLIP) == true

    /**
     * Kotlin extension function to check the number of download transfers that
     * are not background transfers.
     *
     * @return  Number of pending downloads.
     */
    @JvmStatic
    fun ArrayList<MegaTransfer>.getSilentNumPendingDownloads(): Int {
        var count = 0
        forEach { transfer ->
            if (transfer.type == TYPE_DOWNLOAD && !transfer.isBackgroundTransfer()) count++
        }
        return count
    }
}
