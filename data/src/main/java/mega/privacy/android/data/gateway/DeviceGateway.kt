package mega.privacy.android.data.gateway

/**
 * Device gateway
 *
 */
interface DeviceGateway {
    /**
     * Get manufacturer name
     */
    fun getManufacturerName(): String

    /**
     * Get device model
     */
    fun getDeviceModel(): String

    /**
     * Get current device language
     */
    fun getCurrentDeviceLanguage(): String

    /**
     * Get sdk version
     *
     */
    suspend fun getSdkVersion(): Int
}