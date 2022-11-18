package mega.privacy.android.app.meeting.gateway

/**
 * Camera gateway
 */
interface CameraGateway {

    /**
     * Set the front camera
     */
    fun setFrontCamera()

    /**
     * Get front camera
     */
    fun getFrontCamera(): String?

    /**
     * Get back camera
     */
    fun getBackCamera(): String?
}