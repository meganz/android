package mega.privacy.android.app.presentation.photos.timeline.model

/**
 * CameraUploadsStatus enum for various statuses in Photos
 */
enum class CameraUploadsStatus {
    None,
    Sync,
    Uploading,
    Completed,
    Idle,
    Warning,
    Error,
}