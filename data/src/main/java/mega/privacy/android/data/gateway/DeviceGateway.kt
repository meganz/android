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

    /**
     * Get disk space bytes
     *
     * @param path
     */
    suspend fun getDiskSpaceBytes(path: String): Long

    /**
     * Based on the device locale and other preferences, check if times should be
     * formatted as 24 hour times or 12 hour (AM/PM).
     *
     * @return true if 24 hour time format is selected, false otherwise.
     */
    fun is24HourFormat(): Boolean
}