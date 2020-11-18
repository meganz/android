package mega.privacy.android.app.sync.cusync.callback

import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

/**
 * Remove backup event callback.
 */
class RemoveBackupCallback : SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_REMOVE

    override fun onSuccess(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    ) {
        // Remove local cache.
        request.let {
            getDatabase().deleteBackupById(it.parentHandle)
            logDebug("Successful callback: delete ${it.parentHandle}.")
        }
    }

    override fun onFail(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        logWarning("Delete backup with id ${request.parentHandle} failed. Set it as outdated.")
        getDatabase().setBackupAsOutdated(request.parentHandle)
    }
}