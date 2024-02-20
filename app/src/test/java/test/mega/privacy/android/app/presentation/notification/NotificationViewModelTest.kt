package test.mega.privacy.android.app.presentation.notification

import androidx.compose.ui.unit.sp
import app.cash.turbine.test
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.notification.NotificationViewModel
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.mapper.NotificationMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ContactChangeContactEstablishedAlert
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.usecase.AcknowledgeUserAlertsUseCase
import mega.privacy.android.domain.usecase.MonitorUserAlertsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationViewModelTest {
    private lateinit var underTest: NotificationViewModel

    private val monitorUserAlertsUseCase = mock<MonitorUserAlertsUseCase> {
        onBlocking { invoke() }.thenReturn(
            emptyFlow()
        )
    }

    private val acknowledgeUserAlertsUseCase = mock<AcknowledgeUserAlertsUseCase>()

    private val notificationMapper = mock<NotificationMapper>()

    @BeforeEach
    fun setUp() {
        initViewModel()
    }

    private fun initViewModel() {
        underTest = NotificationViewModel(
            acknowledgeUserAlertsUseCase = acknowledgeUserAlertsUseCase,
            monitorUserAlertsUseCase = monitorUserAlertsUseCase,
            notificationMapper = notificationMapper,
        )
    }

    @Test
    fun `test that initial value is an empty list and scroll to top is false`() = runTest {
        underTest.state.test {
            val (notifications, scrollToTop) = awaitItem()
            assertWithMessage("Initial notification list should be empty").that(notifications)
                .isEmpty()
            assertWithMessage("Initial scroll to top should be false").that(scrollToTop).isFalse()
        }
    }

    @Test
    fun `test that subsequent values are returned`() = runTest {

        val expected = Notification(
            sectionTitle = { "" },
            sectionColour = 0,
            sectionIcon = null,
            title = { "" },
            titleTextSize = 16.sp,
            description = { "" },
            schedMeetingNotification = null,
            dateText = { "" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}

        val alert = mock<IncomingPendingContactRequestAlert>()
        whenever(monitorUserAlertsUseCase()).thenReturn(flowOf(listOf(alert)))
        whenever(notificationMapper(alert)).thenReturn(expected)

        initViewModel()

        underTest.state.drop(1).test {
            val (notifications, _) = awaitItem()
            assertWithMessage("Expected returned user alerts").that(notifications)
                .containsExactly(expected)
        }
    }

    @Test
    fun `test that should scroll is updated to true if new items appear`() = runTest {
        val initialAlert = mock<IncomingPendingContactRequestAlert>()
        val newAlert = mock<ContactChangeContactEstablishedAlert>()
        val initialNotification = Notification(
            sectionTitle = { "" },
            sectionColour = 0,
            sectionIcon = null,
            title = { "Initial" },
            titleTextSize = 16.sp,
            description = { "" },
            schedMeetingNotification = null,
            dateText = { "" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}
        val newNotification = initialNotification.copy(title = { "New title" })
        whenever(notificationMapper(initialAlert)).thenReturn(initialNotification)
        whenever(notificationMapper(newAlert)).thenReturn(newNotification)

        whenever(monitorUserAlertsUseCase()).thenReturn(
            flowOf(
                listOf(initialAlert),
                listOf(newAlert, initialAlert)
            )
        )

        initViewModel()

        underTest.state.drop(1).test {
            val (_, scrollToTop) = awaitItem()
            assertWithMessage("Initial scroll value should be false").that(scrollToTop).isFalse()
            val (_, scrollUpdate) = awaitItem()
            assertWithMessage("Subsequent scroll value should be true").that(scrollUpdate).isTrue()
        }
    }

    @Test
    fun `test that notifications are acknowledged once loaded`() {
        underTest.onNotificationsLoaded()
        scheduler.advanceUntilIdle()
        verifyBlocking(acknowledgeUserAlertsUseCase, AcknowledgeUserAlertsUseCase::invoke)
    }

    companion object {
        private val scheduler = TestCoroutineScheduler()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher(scheduler))
    }
}