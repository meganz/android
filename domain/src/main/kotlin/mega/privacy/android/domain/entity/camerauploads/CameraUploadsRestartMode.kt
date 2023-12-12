package mega.privacy.android.domain.entity.camerauploads

/**
 * Restart mode after the stop of the Camera Uploads worker
 */
enum class CameraUploadsRestartMode {
    /**
     * Reschedule the Camera Uploads Worker to a later time
     */
    Reschedule,

    /**
     * Restart immediately the Camera Uploads Worker after it has been stopped
     */
    RestartImmediately,

    /**
     * Stop the Camera Uploads Worker without disabling the feature
     */
    Stop,

    /**
     * Stop the Camera Uploads Worker and disable the feature
     */
    StopAndDisable,
}
