package mega.privacy.android.feature.sync.domain.usecase.sync

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.feature.sync.data.service.SyncBackgroundService
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import javax.inject.Inject

/**
 * Use case to pause/resume syncs based on the status of device battery and WiFi
 *
 * @param isOnWifiNetworkUseCase        [IsOnWifiNetworkUseCase]
 * @param pauseSyncUseCase              [PauseSyncUseCase]
 * @param resumeSyncUseCase             [ResumeSyncUseCase]
 * @param monitorSyncsUseCase         [MonitorSyncsUseCase]
 * @param isSyncPausedByTheUserUseCase  [IsSyncPausedByTheUserUseCase]
 */
internal class PauseResumeSyncsBasedOnBatteryAndWiFiUseCase @Inject constructor(
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
    private val pauseSyncUseCase: PauseSyncUseCase,
    private val resumeSyncUseCase: ResumeSyncUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val isSyncPausedByTheUserUseCase: IsSyncPausedByTheUserUseCase,
) {

    /**
     * Invoke
     *
     * @param connectedToInternet   True if device is connected to Internet or False otherwise
     * @param syncOnlyByWifi        True if setting to sync only by WiFi is enabled or False otherwise
     * @param batteryInfo           The device [BatteryInfo]
     */
    suspend operator fun invoke(
        connectedToInternet: Boolean,
        syncOnlyByWifi: Boolean,
        batteryInfo: BatteryInfo,
        isFreeAccount: Boolean,
    ) {
        val internetNotAvailable = !connectedToInternet
        val userNotOnWifi = !isOnWifiNetworkUseCase()
        val activeSyncs =
            monitorSyncsUseCase().first().filter { !isSyncPausedByTheUserUseCase(it.id) }
        val isLowBatteryLevel =
            batteryInfo.level < SyncBackgroundService.LOW_BATTERY_LEVEL && !batteryInfo.isCharging

        if (internetNotAvailable || syncOnlyByWifi && userNotOnWifi || isLowBatteryLevel || isFreeAccount) {
            activeSyncs.forEach { pauseSyncUseCase(it.id) }
        } else {
            activeSyncs.forEach { resumeSyncUseCase(it.id) }
        }
    }
}