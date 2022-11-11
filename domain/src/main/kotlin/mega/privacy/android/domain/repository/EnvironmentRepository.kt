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
    fun getDeviceInfo(): DeviceInfo

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
}
