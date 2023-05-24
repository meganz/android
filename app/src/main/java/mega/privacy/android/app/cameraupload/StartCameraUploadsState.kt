package mega.privacy.android.app.cameraupload

/**
 * Enum class that represents a state for Camera Uploads upon enabling the feature
 */
enum class StartCameraUploadsState {
    /**
     * Camera Uploads can be ran
     */
    CAN_RUN_CAMERA_UPLOADS,

    /**
     * The Preferences does not exist
     */
    MISSING_PREFERENCES,

    /**
     * The Camera Uploads Sync is disabled
     */
    DISABLED_SYNC,

    /**
     * The device is below the minimum percentage required
     */
    BELOW_DEVICE_BATTERY_LEVEL,

    /**
     * The User does not meet certain Internet conditions
     */
    UNSATISFIED_WIFI_CONSTRAINT,

    /**
     * The Primary Folder does not exist or is invalid
     */
    INVALID_PRIMARY_FOLDER,

    /**
     * The Secondary Folder does not exist. This is only called when Secondary uploads are enabled
     */
    MISSING_SECONDARY_FOLDER,

    /**
     * The User is logged out
     */
    LOGGED_OUT_USER,

    /**
     * The Camera Uploads attribute does not exist
     */
    MISSING_USER_ATTRIBUTE,

    /**
     * The Primary and/or Secondary Folder does not exist
     */
    UNESTABLISHED_FOLDERS,
}
