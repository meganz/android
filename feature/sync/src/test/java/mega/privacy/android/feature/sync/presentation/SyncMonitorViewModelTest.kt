package mega.privacy.android.feature.sync.presentation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.ui.SyncMonitorViewModel
import mega.privacy.android.shared.sync.domain.IsSyncFeatureEnabledUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncMonitorViewModelTest {

    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase = mock()
    private val handleTransferEventUseCase: HandleTransferEventUseCase = mock()
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()
    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase =
        mock()
    private val isSyncFeatureEnabledUseCase: IsSyncFeatureEnabledUseCase = mock {
        on { invoke() }.thenReturn(true)
    }
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase = mock()
    private val getSyncNotificationUseCase: GetSyncNotificationUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()
    private val removeSyncUseCase: RemoveFolderPairUseCase = mock()

    private lateinit var underTest: SyncMonitorViewModel

    @BeforeEach
    fun setup() {
        whenever(monitorTransferEventsUseCase()).thenReturn(flowOf())
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf())
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf())
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf())
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf())
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(flowOf())
        whenever(monitorSyncsUseCase()).thenReturn(flowOf())
    }

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorTransferEventsUseCase,
            handleTransferEventUseCase,
            monitorConnectivityUseCase,
            monitorSyncByWiFiUseCase,
            monitorBatteryInfoUseCase,
            monitorAccountDetailUseCase,
            pauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
            monitorSyncStalledIssuesUseCase,
            monitorSyncsUseCase,
            setSyncNotificationShownUseCase,
            getSyncNotificationUseCase,
            isOnWifiNetworkUseCase,
            removeSyncUseCase
        )
    }

    @Test
    fun `test that monitorTransferEventsUseCase calls handleTransferEventUseCase`() = runTest {
        val transfer: Transfer = mock {
            on { isSyncTransfer } doReturn true
        }
        val transferEvent = TransferEvent.TransferFinishEvent(transfer, null)
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flow {
                emit(transferEvent)
            }
        )

        initViewModel()
        underTest.startMonitoring()
        advanceUntilIdle()

        verify(handleTransferEventUseCase).invoke(
            *arrayOf(transferEvent)
        )
    }

    @Test
    fun `test that monitor sync state updates sync state`() = runTest {
        val batteryInfo = mock<BatteryInfo>()
        val accountLevelDetails: AccountLevelDetail = mock {
            on { accountType } doReturn AccountType.FREE
        }
        val accountDetail: AccountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetails
        }
        val connectedToInternet = true
        val syncByWifi = true
        whenever(monitorConnectivityUseCase()).thenReturn(
            flowOf(connectedToInternet)
        )
        whenever(monitorSyncByWiFiUseCase()).thenReturn(
            flowOf(syncByWifi)
        )
        whenever(monitorBatteryInfoUseCase()).thenReturn(
            flowOf(batteryInfo)
        )
        whenever(monitorAccountDetailUseCase()).thenReturn(
            flowOf(accountDetail)
        )

        initViewModel()
        underTest.startMonitoring()

        verify(pauseResumeSyncsBasedOnBatteryAndWiFiUseCase).invoke(
            connectedToInternet,
            syncByWifi,
            batteryInfo,
            isFreeAccount = true
        )
    }

    @Test
    fun `test that monitor notifications updates sync state`() = runTest {
        val batteryInfo = mock<BatteryInfo>()
        val connectedToInternet = true
        val syncByWifi = true
        val isOnWifi = false
        val isBatteryLow = true
        val syncs = emptyList<FolderPair>()
        val stalledIssues = emptyList<StalledIssue>()
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.BATTERY_LOW,
            notificationDetails = NotificationDetails(path = "", errorCode = 0)
        )
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(
            flowOf(stalledIssues)
        )
        whenever(monitorSyncsUseCase()).thenReturn(
            flowOf(syncs)
        )
        whenever(monitorConnectivityUseCase()).thenReturn(
            flowOf(connectedToInternet)
        )
        whenever(monitorSyncByWiFiUseCase()).thenReturn(
            flowOf(syncByWifi)
        )
        whenever(monitorBatteryInfoUseCase()).thenReturn(
            flowOf(batteryInfo)
        )
        whenever(isOnWifiNetworkUseCase()).thenReturn(
            isOnWifi
        )
        whenever(
            getSyncNotificationUseCase(
                isBatteryLow = isBatteryLow,
                isUserOnWifi = isOnWifi,
                isSyncOnlyByWifi = syncByWifi,
                syncs = syncs,
                stalledIssues = stalledIssues
            )
        ).thenReturn(
            notificationMessage
        )

        initViewModel()
        underTest.startMonitoring()

        assertThat(underTest.state.value.displayNotification).isEqualTo(notificationMessage)
    }

    @Test
    fun `test that onNotificationShown removes notification`() = runTest {
        val notificationMessage = SyncNotificationMessage(
            title = "Notification title",
            text = "Notification text",
            syncNotificationType = SyncNotificationType.BATTERY_LOW,
            notificationDetails = NotificationDetails(path = "", errorCode = 0)
        )

        initViewModel()
        underTest.onNotificationShown(notificationMessage)

        assertThat(underTest.state.value.displayNotification).isNull()
    }

    private fun initViewModel() {
        underTest = SyncMonitorViewModel(
            monitorTransferEventsUseCase,
            handleTransferEventUseCase,
            monitorConnectivityUseCase,
            monitorSyncByWiFiUseCase,
            monitorBatteryInfoUseCase,
            monitorAccountDetailUseCase,
            pauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
            monitorSyncStalledIssuesUseCase,
            monitorSyncsUseCase,
            setSyncNotificationShownUseCase,
            getSyncNotificationUseCase,
            isOnWifiNetworkUseCase,
            isSyncFeatureEnabledUseCase,
            removeSyncUseCase
        )
    }
}