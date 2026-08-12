package mega.privacy.android.feature.sync.domain.usecase.sync.option

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.environment.MonitorPowerSaveModeUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Use case for determining which condition is currently preventing syncs from running:
 * internet connectivity, WiFi settings, battery level, charging state,
 * and power save mode (battery saver)
 */
class MonitorSyncPauseReasonUseCase @Inject constructor(
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase,
    private val monitorPauseSyncOnBatterySaverUseCase: MonitorPauseSyncOnBatterySaverUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val monitorPowerSaveModeUseCase: MonitorPowerSaveModeUseCase,
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) {

    /**
     * Determines which condition is blocking syncs, out of internet connectivity, WiFi settings,
     * battery level, charging state, and power save mode (battery saver)
     *
     * @return Flow<SyncPauseReason?> emitting the blocking condition, or null when sync is allowed
     */
    @OptIn(FlowPreview::class)
    operator fun invoke(): Flow<SyncPauseReason?> = combine(
        monitorBatteryInfoUseCase().distinctUntilChanged(),
        monitorSyncByWiFiUseCase().distinctUntilChanged(),
        monitorSyncByChargingUseCase().distinctUntilChanged(),
        monitorBatterySaverPause(),
        monitorConnectivityUseCase().sample(1.seconds)
    ) { batteryInfo, wiFiOnly, chargingOnly, batterySaverPause, isNetworkChanged ->
        val isUserOnWifi = runCatching { isOnWifiNetworkUseCase() }.getOrDefault(false)
        val reason = when {
            batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging -> SyncPauseReason.LowBattery
            // Charging exempts battery saver, as it does the low battery level
            batterySaverPause && !batteryInfo.isCharging -> SyncPauseReason.BatterySaver
            wiFiOnly && !isUserOnWifi -> SyncPauseReason.NoWifi
            chargingOnly && !batteryInfo.isCharging -> SyncPauseReason.NotCharging
            else -> null
        }
        Timber.d("MonitorSyncPauseReasonUseCase: reason=$reason isNetworkChanged = $isNetworkChanged")

        reason
    }

    private fun monitorBatterySaverPause(): Flow<Boolean> = combine(
        monitorPauseSyncOnBatterySaverUseCase().distinctUntilChanged(),
        monitorPowerSaveModeUseCase().distinctUntilChanged(),
    ) { pauseOnBatterySaver, isInPowerSaveMode ->
        pauseOnBatterySaver && isInPowerSaveMode
    }

    companion object {
        /**
         * Low battery level threshold
         */
        const val LOW_BATTERY_LEVEL = 20
    }
}
