package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncIssueNotificationByTypeUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationTypeUseCase
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncIssueNotificationViewModelTest {

    private val monitorSyncNotificationTypeUseCase: MonitorSyncNotificationTypeUseCase = mock()
    private val getSyncIssueNotificationByTypeUseCase: GetSyncIssueNotificationByTypeUseCase =
        mock()

    private lateinit var underTest: SyncIssueNotificationViewModel
    private val notificationType = SyncNotificationType.NOT_CONNECTED_TO_WIFI
    private val notificationMessage = mock<SyncNotificationMessage>()
    private val notificationTypeFlow = MutableSharedFlow<SyncNotificationType>()

    @BeforeEach
    fun setup() {
        whenever(monitorSyncNotificationTypeUseCase()).thenReturn(notificationTypeFlow)
        whenever(getSyncIssueNotificationByTypeUseCase(notificationType)).thenReturn(
            notificationMessage
        )
        underTest = SyncIssueNotificationViewModel(
            monitorSyncNotificationTypeUseCase = monitorSyncNotificationTypeUseCase,
            getSyncIssueNotificationByTypeUseCase = getSyncIssueNotificationByTypeUseCase
        )
    }

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorSyncNotificationTypeUseCase,
            getSyncIssueNotificationByTypeUseCase
        )
    }

    @Disabled
    @Test
    fun `test that state updates when notification type is NOT_CONNECTED_TO_WIFI`() = runTest {
        notificationTypeFlow.emit(notificationType)
        underTest.state.test {
            val updatedState = awaitItem()
            Truth.assertThat(updatedState.displayNotification).isEqualTo(notificationMessage)
            Truth.assertThat(updatedState.syncNotificationType).isEqualTo(notificationType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that dismissNotification clears the state`() = runTest {
        underTest.dismissNotification()

        underTest.state.test {
            val updatedState = awaitItem()
            Truth.assertThat(updatedState.displayNotification).isNull()
            Truth.assertThat(updatedState.syncNotificationType).isNull()
        }
    }
}
