package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.DeviceInfo

/**
 * Device repository
 *
 */
interface EnvironmentRepository {
    /**
     * Get device info
     *
     * @return device info
     */
    suspend fun getDeviceInfo(): DeviceInfo

    /**
     * Get app info
     *
     * @return app info
     */
    suspend fun getAppInfo(): AppInfo

    /**
     * Get last saved version code
     *
     */
    suspend fun getLastSavedVersionCode(): Int

    /**
     * Get installed version code
     *
     */
    suspend fun getInstalledVersionCode(): Int

    /**
     * save version code
     *
     * @param newVersionCode
     */
    suspend fun saveVersionCode(newVersionCode: Int)

    /**
     * Get device sdk version Int
     *
     */
    suspend fun getDeviceSdkVersionInt(): Int

    /**
     * Get device sdk version name
     *
     */
    suspend fun getDeviceSdkVersionName(): String

    /**
     * Get device memory size in bytes
     *
     * @return memory size in bytes if found
     */
    suspend fun getDeviceMemorySizeInBytes(): Long?

    /**
     * Get is first launch
     *
     * @return first launch value, or null if not set
     */
    suspend fun getIsFirstLaunch(): Boolean?

    /**
     * get current time
     */
    val now: Long

    /**
     * get nano time
     */
    val nanoTime: Long

    /**
     * Get Local Ip Address
     * @return [String]
     */
    suspend fun getLocalIpAddress(): String?

    /**
     * set ip address
     * @param ipAddress [String]
     */
    fun setIpAddress(ipAddress: String?)

    /**
     * get ip address
     * @return ip address [String]
     */
    fun getIpAddress(): String?
}
