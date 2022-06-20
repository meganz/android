package mega.privacy.android.app.domain.repository

import mega.privacy.android.app.domain.entity.AppInfo
import mega.privacy.android.app.domain.entity.DeviceInfo

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
