package mega.privacy.android.app.sync.cusync.callback

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.SettingsConstants.ACTION_REENABLE_CAMERA_UPLOADS_PREFERENCE
import mega.privacy.android.app.sync.Backup
import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.sync.cusync.CuSyncManager.Companion.NAME_OTHER
import mega.privacy.android.app.sync.cusync.CuSyncManager.Companion.NAME_PRIMARY
import mega.privacy.android.app.sync.cusync.CuSyncManager.Companion.NAME_SECONDARY
import mega.privacy.android.app.sync.cusync.CuSyncManager.Companion.TYPE_BACKUP_PRIMARY
import mega.privacy.android.app.sync.cusync.CuSyncManager.Companion.TYPE_BACKUP_SECONDARY
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


open class SetBackupCallback : SyncEventCallback {

    override fun requestType(): Int = MegaRequest.TYPE_BACKUP_PUT

    override fun onSuccess(
        api: MegaApiJava?,
        request: MegaRequest?,
        error: MegaError?
    ) {
        val backupName = when (request?.totalBytes?.toInt()) {
            TYPE_BACKUP_PRIMARY -> NAME_PRIMARY
            TYPE_BACKUP_SECONDARY -> NAME_SECONDARY
            else -> NAME_OTHER
        }

        request?.apply {
            val backup = Backup(
                backupId = parentHandle,
                backupType = totalBytes.toInt(),
                targetNode = nodeHandle,
                localFolder = file,
                deviceId = name,
                state = access,
                subState = numDetails,
                extraData = text,
                name = backupName
            )
            LogUtil.logDebug("Save back $backup to local cache.")
            getDatabase().saveSyncPair(backup)
        }
    }

    override fun onFail(request: MegaRequest?, error: MegaError?) {
        super.onFail(request, error)
        reEnableCameraUploadsPreference()
    }

    private fun reEnableCameraUploadsPreference() =
        LocalBroadcastManager.getInstance(MegaApplication.getInstance())
            .sendBroadcast(Intent(ACTION_REENABLE_CAMERA_UPLOADS_PREFERENCE))
}