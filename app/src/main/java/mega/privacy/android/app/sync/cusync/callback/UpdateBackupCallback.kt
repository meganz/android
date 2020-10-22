package mega.privacy.android.app.sync.cusync.callback

import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


class UpdateBackupCallback: SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_PUT

    override fun onSuccess(
        api: MegaApiJava?,
        request: MegaRequest?,
        error: MegaError?
    ) {
        request?.let {
            val backup = getDatabase().getBackupById(it.parentHandle)

            if (backup != null && !backup.outdated) {
                backup.apply {
                    targetNode = it.nodeHandle
                    localFolder = it.file
                    state = it.access
                    subState = it.numDetails
                }
                getDatabase().updateBackup(backup)
                logDebug("Successful callback: update $backup.")
            }
        }
    }
}