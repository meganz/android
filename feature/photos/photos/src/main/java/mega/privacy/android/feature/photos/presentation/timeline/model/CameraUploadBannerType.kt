package mega.privacy.android.feature.photos.presentation.timeline.model

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
    PendingCount,

    /**
     * Banner to show when Camera Uploads is paused due to the device charging requirement not met
     */
    DeviceChargingNotMet,

    /**
     * Banner to show when Camera Uploads is paused due to the device being on Low Battery
     */
    LowBattery,

    /**
     * Banner to show when Camera Uploads is paused due to the network requirement not met
     */
    NetworkRequirementNotMet,

    /**
     * Banner to show when Camera Uploads is paused due to insufficient storage
     */
    FullStorage
}