package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.MegaApplication
import nz.mega.sdk.MegaTransfer

class CancelTransferListener(context: Context?) : BaseListener(context) {

    /**
     * Cancels a list of transfers.
     *
     * @param selectedTransfers The list of tranfers to cancel.
     */
    fun cancelTransfers(selectedTransfers: List<MegaTransfer>) {
        val megaApi = MegaApplication.getInstance().megaApi

        for (transfer in selectedTransfers) {
            megaApi.cancelTransfer(transfer, this)
        }
    }
}