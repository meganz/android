package mega.privacy.android.app.sync.camerauploads.callback

import mega.privacy.android.app.sync.SyncEventCallback
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber

/**
 * Remove backup event callback.
 */
class RemoveBackupCallback : SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_REMOVE

    override fun onSuccess(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError,
    ) {
        // Remove local cache.
        request.let {
            getDatabase().deleteBackupById(it.parentHandle)
            Timber.d("Successful callback: delete ${it.parentHandle}.")
        }
    }

    override fun onFail(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        Timber.w("Delete backup with id ${request.parentHandle} failed. Set it as outdated.")
        getDatabase().setBackupAsOutdated(request.parentHandle)
    }
}