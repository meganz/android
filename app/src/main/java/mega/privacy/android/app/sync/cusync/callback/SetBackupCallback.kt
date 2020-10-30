package mega.privacy.android.app.sync.cusync.callback

import mega.privacy.android.app.sync.Backup
import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.sync.cusync.CuSyncManager
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


open class SetBackupCallback : SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_PUT

    override fun onSuccess(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    ) {
        request.apply {
            val backup = Backup(
                backupId = parentHandle,
                backupType = totalBytes.toInt(),
                targetNode = nodeHandle,
                localFolder = file,
                backupName = name,
                state = access,
                subState = numDetails,
                extraData = text
            )
            LogUtil.logDebug("Save back $backup to local cache.")
            getDatabase().saveBackup(backup)
            CuSyncManager.reEnableCameraUploadsPreference(totalBytes.toInt())
        }
    }

    override fun onFail(request: MegaRequest, error: MegaError) {
        super.onFail(request, error)
        CuSyncManager.reEnableCameraUploadsPreference(request.totalBytes.toInt())
    }
}