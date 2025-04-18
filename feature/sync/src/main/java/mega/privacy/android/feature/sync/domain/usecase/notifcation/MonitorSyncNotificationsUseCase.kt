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
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase.Companion.LOW_BATTERY_LEVEL
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import javax.inject.Inject

class MonitorSyncNotificationTypeUseCase @Inject constructor(
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getSyncNotificationTypeUseCase: GetSyncNotificationTypeUseCase,
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase,
) {
    operator fun invoke() = combine(
        monitorSyncStalledIssuesUseCase(),
        monitorSyncsUseCase(),
        monitorBatteryInfoUseCase(),
        monitorSyncByWiFiUseCase(),
        monitorConnectivityUseCase()
    ) { stalledIssues: List<StalledIssue>, syncs: List<FolderPair>, batteryInfo: BatteryInfo, syncByWifi: Boolean, isConnectedToInternet ->
        getSyncNotificationTypeUseCase(
            isBatteryLow = batteryInfo.level < LOW_BATTERY_LEVEL && !batteryInfo.isCharging,
            isUserOnWifi = isConnectedToInternet && isOnWifiNetworkUseCase(),
            isSyncOnlyByWifi = syncByWifi,
            syncs = syncs,
            stalledIssues = stalledIssues
        )
    }.distinctUntilChanged()
}
