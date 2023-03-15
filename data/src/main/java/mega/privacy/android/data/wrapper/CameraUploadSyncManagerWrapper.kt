package mega.privacy.android.data.wrapper

import mega.privacy.android.domain.entity.BackupState

/**
 * The interface for wrapping static [CameraUploadSyncManager] methods.
 */
interface CameraUploadSyncManagerWrapper {

    /**
     * Wrapper method that calls [CameraUploadSyncManager.doRegularHeartbeat]
     */
    fun doRegularHeartbeat()

    /**
     * Wrapper method that calls [CameraUploadSyncManager.updatePrimaryFolderBackupState]
     *
     * @param backupState [BackupState]
     */
    fun updatePrimaryFolderBackupState(backupState: BackupState)

    /**
     * Wrapper method that calls [CameraUploadSyncManager.updateSecondaryFolderBackupState]
     *
     * @param backupState [BackupState]
     */
    fun updateSecondaryFolderBackupState(backupState: BackupState)
}
