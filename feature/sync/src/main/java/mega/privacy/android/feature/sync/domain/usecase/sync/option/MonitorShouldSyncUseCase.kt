package mega.privacy.android.feature.sync.domain.usecase.sync.option

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for determining if sync should be allowed based on all conditions:
 * internet connectivity, WiFi settings, battery level, and charging state
 */
class MonitorShouldSyncUseCase @Inject constructor(
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
) {

    /**
     * Determines if sync should be allowed based on all conditions including internet connectivity,
     * WiFi settings, battery level, and charging state
     *
     * @return Flow<Boolean> indicating if sync should be allowed
     */
    operator fun invoke(): Flow<Boolean> = combine(
        monitorBatteryInfoUseCase().distinctUntilChanged(),
        monitorSyncByWiFiUseCase().distinctUntilChanged(),
        monitorSyncByChargingUseCase().distinctUntilChanged(),
    ) { batteryInfo, wiFiOnly, chargingOnly ->
        val isUserOnWifi = runCatching { isOnWifiNetworkUseCase() }.getOrDefault(false)
        val wifiAllowed = checkWifiSettings(isUserOnWifi, wiFiOnly)
        val batteryAllowed = checkBatterySettings(batteryInfo.isCharging, chargingOnly)
        val batteryLevelAllowed = checkBatteryLevel(batteryInfo)
        Timber.d("MonitorShouldSyncUseCase: wifiAllowed=$wifiAllowed, batteryAllowed=$batteryAllowed, batteryLevelAllowed=$batteryLevelAllowed")

        wifiAllowed && batteryAllowed && batteryLevelAllowed
    }

    /**
     * Checks if sync is allowed based on WiFi settings
     */
    private fun checkWifiSettings(isOnWiFi: Boolean, wiFiOnly: Boolean): Boolean {
        return if (wiFiOnly) {
            isOnWiFi // Only allow sync on WiFi
        } else {
            true // Allow sync on both WiFi and mobile data
        }
    }

    /**
     * Checks if sync is allowed based on battery settings
     */
    private fun checkBatterySettings(
        isCharging: Boolean,
        chargingOnly: Boolean,
    ): Boolean {
        // If charging-only is enabled, must be charging
        return if (chargingOnly) {
            isCharging
        } else {
            true // Allow sync regardless of charging state
        }
    }

    /**
     * Checks if sync is allowed based on battery level
     */
    private fun checkBatteryLevel(batteryInfo: mega.privacy.android.domain.entity.BatteryInfo): Boolean {
        val isLowBattery = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging
        return !isLowBattery
    }

    companion object {
        /**
         * Low battery level threshold
         */
        const val LOW_BATTERY_LEVEL = 20
    }
}
