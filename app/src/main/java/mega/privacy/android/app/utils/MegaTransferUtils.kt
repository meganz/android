package mega.privacy.android.app.utils

import mega.privacy.android.app.data.extensions.isBackgroundTransfer
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD

object MegaTransferUtils {

    /**
     * Get the number of pending download transfers that are not background transfers.
     *
     * @return  Number of pending downloads.
     */
    @JvmStatic
    fun MegaApiAndroid.getNumPendingDownloadsNonBackground(): Int =
        getTransfers(TYPE_DOWNLOAD)
            ?.count { transfer -> !transfer.isFinished && !transfer.isBackgroundTransfer() } ?: 0
}
