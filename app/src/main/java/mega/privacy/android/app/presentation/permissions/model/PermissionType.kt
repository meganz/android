package mega.privacy.android.app.presentation.permissions.model

/**
 * Enum class defining the permission type to ask.
 */
enum class PermissionType {

    /**
     * Notifications permission.
     */
    Notifications,

    /**
     * Display over other apps permission.
     */
    DisplayOverOtherApps,

    /**
     * Read and write permissions.
     */
    ReadAndWrite,

    /**
     * Write storage permission.
     */
    Write,

    /**
     * Read storage permission.
     */
    Read,

    /**
     * Camera permission.
     */
    Camera,

    /**
     * Microphone and bluetooth permissions.
     */
    MicrophoneAndBluetooth,

    /**
     * Microphone permission.
     */
    Microphone,

    /**
     * Bluetooth permission.
     */
    Bluetooth,

    /**
     * Camera Backup permission
     */
    CameraBackup
}