package mega.privacy.android.app.meeting.gateway

/**
 * Gateway that exposes which camera is currently in use, hiding the JNI-backed
 * `VideoCaptureUtils` static behind an injectable, mockable interface.
 */
interface CameraInUseGateway {

    /**
     * Check whether the front camera is the video device currently in use.
     *
     * @return True if the front camera is in use, false otherwise.
     */
    fun isFrontCameraInUse(): Boolean
}
