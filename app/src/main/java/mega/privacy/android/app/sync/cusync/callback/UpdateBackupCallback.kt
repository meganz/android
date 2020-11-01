package mega.privacy.android.app.sync.cusync.callback

import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


class UpdateBackupCallback: SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_PUT

    override fun onSuccess(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    ) {
        request.apply {
            val backup = getDatabase().getBackupById(parentHandle)

            if (backup != null && !backup.outdated) {
                backup.apply {
                    targetNode = nodeHandle
                    localFolder = file
                    state = access
                    subState = numDetails
                }
                getDatabase().updateBackup(backup)
                logDebug("Successful callback: update $backup.")
            }
        }
    }
}