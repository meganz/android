package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.usecase.notifcation.GetSyncIssueNotificationByTypeUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.MonitorSyncNotificationTypeUseCase
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SyncIssueNotificationViewModelTest {

    private val monitorSyncNotificationTypeUseCase: MonitorSyncNotificationTypeUseCase = mock()
    private val getSyncIssueNotificationByTypeUseCase: GetSyncIssueNotificationByTypeUseCase =
        mock()

    private lateinit var underTest: SyncIssueNotificationViewModel
    private val notificationType = SyncNotificationType.NOT_CONNECTED_TO_WIFI
    private val notificationMessage = mock<SyncNotificationMessage>()

    @BeforeEach
    fun setup() {
        whenever(monitorSyncNotificationTypeUseCase()).thenReturn(flowOf(notificationType))
        whenever(getSyncIssueNotificationByTypeUseCase(notificationType)).thenReturn(
            notificationMessage
        )
        underTest = SyncIssueNotificationViewModel(
            monitorSyncNotificationTypeUseCase = monitorSyncNotificationTypeUseCase,
            getSyncIssueNotificationByTypeUseCase = getSyncIssueNotificationByTypeUseCase
        )
    }

    @Test
    fun `test that state updates when notification type is NOT_CONNECTED_TO_WIFI`() = runTest {

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
