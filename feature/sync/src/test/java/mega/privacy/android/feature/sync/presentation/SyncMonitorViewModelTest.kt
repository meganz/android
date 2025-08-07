package mega.privacy.android.feature.sync.presentation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.ui.SyncMonitorViewModel
import mega.privacy.android.shared.resources.R as sharedResR
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
internal class SyncMonitorViewModelTest {

    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase = mock()
    private val handleTransferEventUseCase: HandleTransferEventUseCase = mock()
    private val monitorShouldSyncUseCase: MonitorShouldSyncUseCase = mock()
    private val monitorSyncNotificationsUseCase: MonitorSyncNotificationsUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase: PauseResumeSyncsBasedOnBatteryAndWiFiUseCase =
        mock()
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase = mock()
    private val getSyncNotificationUseCase: GetSyncNotificationUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()

    private lateinit var underTest: SyncMonitorViewModel

    @BeforeEach
    fun setup() {
        whenever(monitorTransferEventsUseCase()).thenReturn(emptyFlow())
        whenever(monitorShouldSyncUseCase()).thenReturn(emptyFlow())
        whenever(monitorSyncNotificationsUseCase()).thenReturn(emptyFlow())
        whenever(monitorBatteryInfoUseCase()).thenReturn(emptyFlow())
        whenever(monitorSyncStalledIssuesUseCase()).thenReturn(emptyFlow())
        whenever(monitorSyncsUseCase()).thenReturn(emptyFlow())
    }

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorTransferEventsUseCase,
            handleTransferEventUseCase,
            monitorBatteryInfoUseCase,
            pauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
            monitorSyncStalledIssuesUseCase,
            monitorSyncsUseCase,
            setSyncNotificationShownUseCase,
            getSyncNotificationUseCase,
            isOnWifiNetworkUseCase,
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
        whenever(monitorShouldSyncUseCase()).thenReturn(flowOf(true))
        initViewModel()
        underTest.startMonitoring()

        verify(pauseResumeSyncsBasedOnBatteryAndWiFiUseCase).invoke(true)
    }

    @Test
    fun `test that monitor notifications updates sync state`() = runTest {
        val notificationMessage = SyncNotificationMessage(
            title = sharedResR.string.general_sync_notification_low_battery_title,
            text = sharedResR.string.general_sync_notification_low_battery_text,
            syncNotificationType = SyncNotificationType.BATTERY_LOW,
            notificationDetails = NotificationDetails(path = "", errorCode = 0)
        )
        whenever(monitorSyncNotificationsUseCase()).thenReturn(flowOf(notificationMessage))

        initViewModel()
        underTest.startMonitoring()

        assertThat(underTest.state.value.displayNotification).isEqualTo(notificationMessage)
    }

    @Test
    fun `test that onNotificationShown removes notification`() = runTest {
        val notificationId = 1234
        val notificationMessage = SyncNotificationMessage(
            title = sharedResR.string.general_sync_notification_low_battery_title,
            text = sharedResR.string.general_sync_notification_low_battery_text,
            syncNotificationType = SyncNotificationType.BATTERY_LOW,
            notificationDetails = NotificationDetails(path = "", errorCode = 0)
        )

        initViewModel()
        underTest.onNotificationShown(notificationMessage, notificationId)

        assertThat(underTest.state.value.displayNotification).isNull()
    }

    private fun initViewModel() {
        underTest = SyncMonitorViewModel(
            monitorTransferEventsUseCase = monitorTransferEventsUseCase,
            handleTransferEventUseCase = handleTransferEventUseCase,
            monitorShouldSyncUseCase = monitorShouldSyncUseCase,
            monitorSyncNotificationsUseCase = monitorSyncNotificationsUseCase,
            pauseResumeSyncsBasedOnBatteryAndWiFiUseCase = pauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
            setSyncNotificationShownUseCase = setSyncNotificationShownUseCase,
        )
    }
}
