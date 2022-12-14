package mega.privacy.android.data.gateway

/**
 * Camera gateway
 *
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