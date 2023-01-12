package mega.privacy.android.data.gateway

/**
 * Device gateway
 *
 * @constructor Create empty Device gateway
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
     * Get sdk version int
     *
     */
    suspend fun getSdkVersionInt(): Int

    /**
     * Get sdk version name
     *
     */
    suspend fun getSdkVersionName(): String

    /**
     * Get current time in millis
     *
     */
    fun getCurrentTimeInMillis(): Long

    /**
     * Elapsed realtime
     *
     */
    fun getElapsedRealtime(): Long

    /**
     * Get device memory
     *
     * @return the total device memory if available
     */
    suspend fun getDeviceMemory(): Long?
}