package mega.privacy.android.app.sync.camerauploads

import mega.privacy.android.app.sync.BackupState

/**
 * The interface for wrapping static [CameraUploadSyncManager] methods.
 */
interface CameraUploadSyncManagerWrapper {

    /**
     * doRegularHeartbeat
     */
    fun doRegularHeartbeat() = CameraUploadSyncManager.doRegularHeartbeat()

    /**
     * updatePrimaryFolderBackupState
     * @param backupState [BackupState]
     */
    fun updatePrimaryFolderBackupState(backupState: BackupState) =
        CameraUploadSyncManager.updatePrimaryFolderBackupState(backupState)

    /**
     * updateSecondaryFolderBackupState
     * @param backupState [BackupState]
     */
    fun updateSecondaryFolderBackupState(backupState: BackupState) =
        CameraUploadSyncManager.updateSecondaryFolderBackupState(backupState)
}
