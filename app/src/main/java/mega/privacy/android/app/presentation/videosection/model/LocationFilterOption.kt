package mega.privacy.android.app.presentation.videosection.model

/**
 * Enum class to represent the location filter option.
 *
 * @param title The title of the filter option.
 */
enum class LocationFilterOption(val title: String) {

    /**
     * Cloud drive filter option.
     */
    CloudDrive("Cloud drive"),

    /**
     * Camera uploads filter option.
     */
    CameraUploads("Camera uploads"),

    /**
     * Shared items filter option.
     */
    SharedItems("Shared items"),
}