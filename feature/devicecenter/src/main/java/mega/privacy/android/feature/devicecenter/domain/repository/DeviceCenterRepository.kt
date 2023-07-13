package mega.privacy.android.feature.devicecenter.domain.repository

/**
 * Repository class that provides several functions for Device Center Use Cases
 */
interface DeviceCenterRepository {

    /**
     * Sets a name to the current Device
     *
     * @param deviceName The Device Name
     */
    suspend fun setDeviceName(deviceName: String)

    /**
     * Retrieves the Device ID of the current Device
     *
     * @return the Device ID
     */
    suspend fun getDeviceId(): String?
}