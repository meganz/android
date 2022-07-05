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
}
