package mega.privacy.android.app.sync.camerauploads

/**
 * The interface for wrapping static [CameraUploadSyncManager] methods.
 */
interface CameraUploadSyncManagerWrapper {
    fun doRegularHeartbeat() = CameraUploadSyncManager.doRegularHeartbeat()
}
