package mega.privacy.android.data.gateway

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo

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
    fun getSdkVersionInt(): Int

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
     * Get current hour of day
     *
     * @return hour of day
     */
    fun getCurrentHourOfDay(): Int


    /**
     * Get current minute
     *
     * @return minute
     */
    fun getCurrentMinute(): Int

    /**
     * Get battery info
     */
    fun getBatteryInfo(intent: Intent?): BatteryInfo

    /**
     * Monitor battery info
     */
    val monitorBatteryInfo: Flow<BatteryInfo>

    /**
     * Monitors the Device Power Connection state
     */
    val monitorDevicePowerConnectionState: Flow<String?>

    /**
     * Monitor thermal state
     */
    val monitorThermalState: Flow<Int>

    /**
     * Get the number of available processors
     */
    fun getAvailableProcessors(): Int

    /**
     * Get current timezone
     */
    fun getTimezone(): String
}
