package mega.privacy.android.app.sync

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import mega.privacy.android.app.utils.CameraUploadUtil

/**
 * @return Name of the node with the handle. null if the node doesn't exist.
 */
fun Long.name(): String? = MegaApplication.getInstance().megaApi.getNodeByHandle(this)?.name

/**
 * When user tries to logout, should delete backups first.
 * This should be called before megaApi.logout().
 */
fun removeBackupsBeforeLogout() {
    if (CameraUploadUtil.isPrimaryEnabled()) {
        CameraUploadSyncManager.removePrimaryBackup()
    }

    if (CameraUploadUtil.isSecondaryEnabled()) {
        CameraUploadSyncManager.removeSecondaryBackup()
    }
}