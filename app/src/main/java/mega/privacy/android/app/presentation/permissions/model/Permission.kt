package mega.privacy.android.app.presentation.permissions.model

/**
 * Requested permissions in app.
 */
enum class Permission {

    /**
     * Notifications permission.
     */
    Notifications,

    /**
     * Display over other apps permission.
     */
    DisplayOverOtherApps,

    /**
     * Read permission.
     */
    Read,

    /**
     * Write permission.
     */
    Write,

    /**
     * Camera permission.
     */
    Camera,

    /**
     * Microphone permission.
     */
    Microphone,

    /**
     * Bluetooth permission.
     */
    Bluetooth,

    /*
    * Camera Backup permission
    */
    CameraBackup,
}