package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.DisplaySyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseResumeSyncsBasedOnBatteryAndWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncMonitorInitialiserTest {
    private lateinit var underTest: SyncMonitorInitialiser

    private val monitorShouldSyncUseCase = mock<MonitorShouldSyncUseCase>()
    private val monitorSyncNotificationsUseCase = mock<MonitorSyncNotificationsUseCase>()
    private val pauseResumeSyncsBasedOnBatteryAndWiFiUseCase =
        mock<PauseResumeSyncsBasedOnBatteryAndWiFiUseCase>()
    private val displaySyncNotificationUseCase = mock<DisplaySyncNotificationUseCase>()

    private val notificationMessage = SyncNotificationMessage(
        title = 1,
        text = 2,
        syncNotificationType = SyncNotificationType.STALLED_ISSUE,
        notificationDetails = NotificationDetails(path = null, errorCode = null),
    )

    @BeforeAll
    fun setUp() {
        underTest = SyncMonitorInitialiser(
            monitorShouldSyncUseCase = monitorShouldSyncUseCase,
            monitorSyncNotificationsUseCase = monitorSyncNotificationsUseCase,
            pauseResumeSyncsBasedOnBatteryAndWiFiUseCase = pauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
            displaySyncNotificationUseCase = displaySyncNotificationUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorShouldSyncUseCase,
            monitorSyncNotificationsUseCase,
            pauseResumeSyncsBasedOnBatteryAndWiFiUseCase,
            displaySyncNotificationUseCase,
        )
        monitorShouldSyncUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorSyncNotificationsUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
    }

    @Test
    fun `test that invoke pauses or resumes syncs for each distinct should sync value`() =
        runTest {
            monitorShouldSyncUseCase.stub { on { invoke() }.thenReturn(flowOf(false, true)) }

            underTest()

            verify(pauseResumeSyncsBasedOnBatteryAndWiFiUseCase).invoke(false)
            verify(pauseResumeSyncsBasedOnBatteryAndWiFiUseCase).invoke(true)
        }

    @Test
    fun `test that invoke shows the sync notification when it is not already displayed`() =
        runTest {
            monitorSyncNotificationsUseCase.stub {
                on { invoke() }.thenReturn(flowOf(notificationMessage))
            }
            displaySyncNotificationUseCase.stub { on { invoke(notificationMessage) }.thenReturn(true) }

            underTest()

            verify(displaySyncNotificationUseCase).invoke(notificationMessage)
        }

    @Test
    fun `test that invoke does not show the sync notification when it is already displayed`() =
        runTest {
            monitorSyncNotificationsUseCase.stub {
                on { invoke() }.thenReturn(flowOf(notificationMessage))
            }
            underTest()

            verify(displaySyncNotificationUseCase).invoke(notificationMessage)
        }

    @Test
    fun `test that invoke does not show the sync notification when the permission is not granted`() =
        runTest {
            monitorSyncNotificationsUseCase.stub {
                on { invoke() }.thenReturn(flowOf(notificationMessage))
            }
            underTest()

            verify(displaySyncNotificationUseCase).invoke(notificationMessage)
        }

    @Test
    fun `test that invoke retries notification monitoring after an upstream failure`() = runTest {
        var attempts = 0
        monitorSyncNotificationsUseCase.stub {
            on { invoke() }.thenReturn(
                flow {
                    if (attempts++ == 0) throw IllegalStateException("temporary failure")
                    emit(notificationMessage)
                }
            )
        }
        displaySyncNotificationUseCase.stub {
            on { invoke(notificationMessage) }.thenReturn(true)
        }

        underTest()

        verify(displaySyncNotificationUseCase).invoke(notificationMessage)
    }
}
