package mega.privacy.android.feature.documentscanner.domain.entity

/**
 * Capture mode for the document scanner.
 */
enum class CaptureMode {
    /** User taps the shutter button to capture */
    MANUAL,

    /** Capture triggers automatically when a stable document is detected */
    AUTO,

    /** Continuous capture on page flip detection without interrupting the camera feed */
    CONTINUOUS,
}
