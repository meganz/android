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
     * The Camera Uploads local path does not exist
     */
    MISSING_LOCAL_PATH,

    /**
     * The User does not meet certain Internet conditions
     */
    UNSATISFIED_WIFI_CONSTRAINT,

    /**
     * The local Primary Folder does not exist
     */
    MISSING_LOCAL_PRIMARY_FOLDER,

    /**
     * The local Secondary Folder does not exist
     */
    MISSING_LOCAL_SECONDARY_FOLDER,

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
