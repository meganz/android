package mega.privacy.android.feature.sync.data.mapper.sync

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationTypeUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationTypeUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MonitorSyncNotificationTypeUseCaseTest {

    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val getSyncNotificationTypeUseCase: GetSyncNotificationTypeUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()

    private val underTest = MonitorSyncNotificationTypeUseCase(
        monitorSyncStalledIssuesUseCase,
        monitorSyncsUseCase,
        monitorBatteryInfoUseCase,
        monitorSyncByWiFiUseCase,
        monitorConnectivityUseCase,
        getSyncNotificationTypeUseCase,
        isOnWifiNetworkUseCase
    )

    @Test
    fun `test emits correct SyncNotificationType`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = true
        val isConnectedToInternet = true

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationTypeUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = true,
                syncs = syncs,
                stalledIssues = stalledIssues
            )
        ).thenReturn(SyncNotificationType.BATTERY_LOW)
        underTest().test {
            val result = awaitItem()
            Truth.assertThat(result).isEqualTo(SyncNotificationType.BATTERY_LOW)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
