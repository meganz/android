package mega.privacy.android.app.presentation.photos.timeline.model

/**
 * Enum class to represent different types of Camera Uploads banners
 */
enum class CameraUploadsBannerType {
    /**
     * Banner type is not set
     */
    NONE,

    /**
     * Banner to show when Camera Uploads is disabled
     */
    EnableCameraUploads,

    /**
     * Banner to show when Camera Uploads is enabled and has limited access
     */
    NoFullAccess,

    /**
     * Banner to show when checking upload records
     */
    CheckingUploads,

    /**
     * Banner to show the pending uploading count
     */
    PendingCount
}