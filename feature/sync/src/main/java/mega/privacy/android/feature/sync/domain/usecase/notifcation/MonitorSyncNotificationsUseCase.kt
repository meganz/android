package mega.privacy.android.feature.sync.domain.usecase.notifcation

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import javax.inject.Inject

class MonitorSyncNotificationsUseCase @Inject constructor(
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
    private val getSyncNotificationUseCase: GetSyncNotificationUseCase,
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase,
) {
    operator fun invoke() = combine(
        monitorSyncStalledIssuesUseCase().distinctUntilChanged(),
        monitorSyncsUseCase().distinctUntilChanged(),
        monitorBatteryInfoUseCase().distinctUntilChanged(),
        monitorSyncByWiFiUseCase().distinctUntilChanged(),
        monitorConnectivityUseCase().distinctUntilChanged(),
        monitorSyncByChargingUseCase().distinctUntilChanged()
    ) { flows ->
        val stalledIssues = flows[0] as List<StalledIssue>
        val syncs = flows[1] as List<FolderPair>
        val batteryInfo = flows[2] as BatteryInfo
        val syncByWifi = flows[3] as Boolean
        val isConnectedToInternet = flows[4] as Boolean
        val isSyncByChargingOnly = flows[5] as Boolean
        getSyncNotificationUseCase(
            isBatteryLow = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging,
            isUserOnWifi = isConnectedToInternet && isOnWifiNetworkUseCase(),
            isSyncOnlyByWifi = syncByWifi,
            syncs = syncs,
            isCharging = batteryInfo.isCharging,
            isSyncOnlyWhenCharging = isSyncByChargingOnly,
            stalledIssues = stalledIssues
        )
    }
}
