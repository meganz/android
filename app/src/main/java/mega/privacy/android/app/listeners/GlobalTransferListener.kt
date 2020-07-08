package mega.privacy.android.app.listeners

import mega.privacy.android.app.utils.AlertsAndWarnings.Companion.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface

class GlobalTransferListener : MegaTransferListenerInterface {
    override fun onTransferData(api: MegaApiJava?, transfer: MegaTransfer?, buffer: ByteArray?): Boolean {
        return true
    }

    override fun onTransferStart(api: MegaApiJava?, transfer: MegaTransfer?) {
        logDebug("Transfer start - Node handle: " + transfer?.nodeHandle)
    }

    override fun onTransferFinish(api: MegaApiJava?, transfer: MegaTransfer?, e: MegaError?) {
        logWarning("Transfer error (" + e?.errorCode + "): " + e?.errorString)
        if (e?.errorCode == MegaError.API_EPAYWALL) {
            showOverDiskQuotaPaywallWarning()
        }
    }

    override fun onTransferUpdate(api: MegaApiJava?, transfer: MegaTransfer?) {

    }

    override fun onTransferTemporaryError(api: MegaApiJava?, transfer: MegaTransfer?, e: MegaError?) {
        logWarning("Transfer error (" + e?.errorCode + "): " + e?.errorString)
        if (e?.errorCode == MegaError.API_EPAYWALL) {
            showOverDiskQuotaPaywallWarning()
        }
    }
}