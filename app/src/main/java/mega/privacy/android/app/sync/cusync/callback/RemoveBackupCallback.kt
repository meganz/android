package mega.privacy.android.app.sync.cusync.callback

import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


class RemoveBackupCallback : SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_REMOVE

    override fun onSuccess(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    ) {
        request.let {
            getDatabase().deleteBackupById(it.parentHandle)
            logDebug("Successful callback: delete ${it.parentHandle}.")
        }
    }

    override fun onFail(request: MegaRequest, error: MegaError) {
        logDebug("Delete sync with id ${request.parentHandle} failed.")
        request.let {
            getDatabase().setBackupAsOutdated(request.parentHandle)
        }
    }
}