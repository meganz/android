package mega.privacy.android.app.sync.cusync.callback

import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


class RemoveBackupCallback : SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_REMOVE

    override fun onSuccess(
        api: MegaApiJava?,
        request: MegaRequest?,
        error: MegaError?
    ) {
        request?.let {
            getDatabase().deleteSyncPairById(it.parentHandle)
            logDebug("Successful callback: delete ${it.parentHandle}.")
            TL.log("Successful callback: delete ${it.parentHandle}.")
        }
    }

    override fun onFail(result: SyncEventResult, error: MegaError?) {
        logDebug("Delete sync with id ${result.syncId} failed.")
        getDatabase().setSyncPairAsOutdated(result.syncId)
    }
}