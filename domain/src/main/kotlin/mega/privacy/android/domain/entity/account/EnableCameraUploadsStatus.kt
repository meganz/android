package mega.privacy.android.domain.entity.account

/**
 * Enum class that denotes different statuses when the user enables Camera Uploads
 */
enum class EnableCameraUploadsStatus {

    /**
     * Enum entry that indicates the user is allowed to enable Camera Uploads
     */
    CAN_ENABLE_CAMERA_UPLOADS,

    /**
     * Enum entry that indicates the regular Business Account prompt should be shown
     */
    SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT,

    /**
     * Enum entry that indicates the Suspended Business Account prompt should be shown
     */
    SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT,
}