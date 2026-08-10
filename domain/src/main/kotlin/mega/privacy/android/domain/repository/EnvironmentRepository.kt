package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.AppInfo
import mega.privacy.android.domain.entity.AppVersion
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.DeviceInfo
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.entity.environment.ThermalState
import java.util.Locale

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
     * Get the consumer friendly device name, always starting with the manufacturer
     * followed by the user set device name.
     *
     * @return device name
     */
    fun getDeviceName(): String

    /**
     * Get app info
     *
     * @return app info
     */
    fun getAppInfo(): AppInfo

    /**
     * Get app version
     *
     * @return app version
     */
    fun getAppVersion(): AppVersion?

    /**
     * Get device sdk version Int
     *
     */
    fun getDeviceSdkVersionInt(): Int

    /**
     * Get device sdk version name
     *
     */
    fun getDeviceSdkVersionName(): String

    /**
     * Get device memory size in bytes
     *
     * @return memory size in bytes if found
     */
    suspend fun getDeviceMemorySizeInBytes(): Long?

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

    /**
     * Monitor thermal state
     */
    fun monitorThermalState(): Flow<ThermalState>

    /**
     * Get battery info
     */
    fun getBatteryInfo(): BatteryInfo

    /**
     * Monitor battery info
     */
    fun monitorBatteryInfo(): Flow<BatteryInfo>

    /**
     * Monitors the Device Power Connection State
     *
     * @return a Flow that observes and returns the Device Power Connection State
     */
    fun monitorDevicePowerConnectionState(): Flow<DevicePowerConnectionState>

    /**
     * Monitor power save mode (Battery Saver)
     *
     * @return a Flow that emits true when power save mode is enabled
     */
    fun monitorPowerSaveMode(): Flow<Boolean>

    /**
     * Get the number of available processors
     */
    fun availableProcessors(): Int

    /**
     * Get historical process exit reasons and log to Timber
     */
    suspend fun getHistoricalProcessExitReasons()

    /**
     * Get timezone
     *
     */
    fun getTimezone(): String

    /**
     * Get current locale
     */
    fun getLocale(): Locale
}
