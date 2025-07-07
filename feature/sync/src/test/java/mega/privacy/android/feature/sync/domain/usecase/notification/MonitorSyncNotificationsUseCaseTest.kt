package mega.privacy.android.feature.sync.domain.usecase.notification

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
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSyncNotificationsUseCaseTest {

    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()
    private val getSyncNotificationUseCase: GetSyncNotificationUseCase = mock()
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase = mock()

    private val underTest = MonitorSyncNotificationsUseCase(
        monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
        monitorSyncsUseCase = monitorSyncsUseCase,
        monitorBatteryInfoUseCase = monitorBatteryInfoUseCase,
        monitorSyncByWiFiUseCase = monitorSyncByWiFiUseCase,
        monitorConnectivityUseCase = monitorConnectivityUseCase,
        isOnWifiNetworkUseCase = isOnWifiNetworkUseCase,
        getSyncNotificationUseCase = getSyncNotificationUseCase,
        monitorSyncByChargingUseCase = monitorSyncByChargingUseCase
    )

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorSyncStalledIssuesUseCase,
            monitorSyncsUseCase,
            monitorBatteryInfoUseCase,
            monitorSyncByWiFiUseCase,
            monitorConnectivityUseCase,
            isOnWifiNetworkUseCase,
            getSyncNotificationUseCase,
            monitorSyncByChargingUseCase,
        )
    }

    @Test
    fun `test that it returns correct notification with normal battery level`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it returns correct notification with low battery level`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 10, isCharging = false)
        val syncByWifi = true
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(false)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = true,
                isUserOnWifi = false,
                isSyncOnlyByWifi = true,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it returns correct notification when charging`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 10, isCharging = true)
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = true
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = true,
                isSyncOnlyWhenCharging = true,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it filters duplicate notifications with distinctUntilChanged`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues, stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs, syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo, batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi, syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(
            flowOf(
                isConnectedToInternet,
                isConnectedToInternet
            )
        )
        whenever(monitorSyncByChargingUseCase()).thenReturn(
            flowOf(
                isSyncByChargingOnly,
                isSyncByChargingOnly
            )
        )
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it with no internet connection`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = false
        val isConnectedToInternet = false
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = false,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it with WiFi required but not connected to WiFi`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = true
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(false)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = false,
                isSyncOnlyByWifi = true,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it returns with charging required but not charging`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = true
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = true,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it emits with low battery level exactly at threshold`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 15, isCharging = false) // LOW_BATTERY_LEVEL = 15
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false, // level = 15 is not < 15
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it emits with low battery level below threshold`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val syncs = listOf<FolderPair>()
        val batteryInfo =
            BatteryInfo(level = 14, isCharging = false) // Below LOW_BATTERY_LEVEL = 15
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = true, // level = 14 is < 15
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it emits with stalled issues`() = runTest {
        val stalledIssue = mock<StalledIssue>()
        val stalledIssues = listOf(stalledIssue)
        val syncs = listOf<FolderPair>()
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }

    @Test
    fun `test that it emits with multiple folder pairs`() = runTest {
        val stalledIssues = listOf<StalledIssue>()
        val folderPair1 = mock<FolderPair>()
        val folderPair2 = mock<FolderPair>()
        val syncs = listOf(folderPair1, folderPair2)
        val batteryInfo = BatteryInfo(level = 50, isCharging = false)
        val syncByWifi = false
        val isConnectedToInternet = true
        val isSyncByChargingOnly = false
        val expectedNotification = null

        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        whenever(monitorSyncsUseCase()).thenReturn(flowOf(syncs))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(syncByWifi))
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(isConnectedToInternet))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(isSyncByChargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(true)
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = false,
                isUserOnWifi = true,
                isSyncOnlyByWifi = false,
                syncs = syncs,
                isCharging = false,
                isSyncOnlyWhenCharging = false,
                stalledIssues = stalledIssues
            )
        ).thenReturn(expectedNotification)

        underTest().test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedNotification)
            awaitComplete()
        }
    }
}
