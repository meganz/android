package mega.privacy.android.app.utils

import mega.privacy.android.app.utils.Constants.APP_DATA_BACKGROUND_TRANSFER
import mega.privacy.android.app.utils.Constants.APP_DATA_VOICE_CLIP
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD

object MegaTransferUtils {

    /**
     * Check whether a MegaTransfer is a background transfer.
     *
     * @return  True if it is, false otherwise.
     */
    @JvmStatic
    fun MegaTransfer.isBackgroundTransfer(): Boolean =
        appData?.contains(APP_DATA_BACKGROUND_TRANSFER) == true

    /**
     * Check whether a MegaTransfer is a voice clip type transfer.
     *
     * @return  True if it is, false otherwise.
     */
    @JvmStatic
    fun MegaTransfer.isVoiceClipType(): Boolean =
        appData?.contains(APP_DATA_VOICE_CLIP) == true

    /**
     * Get the number of pending download transfers that are not background transfers.
     *
     * @return  Number of pending downloads.
     */
    @JvmStatic
    fun MegaApiAndroid.getNumPendingDownloadsNonBackground(): Int =
        getTransfers(TYPE_DOWNLOAD)
            ?.count { !it.isFinished && !it.isBackgroundTransfer() } ?: 0
}
