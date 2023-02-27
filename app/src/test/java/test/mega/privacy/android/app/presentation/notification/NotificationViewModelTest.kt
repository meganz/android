package test.mega.privacy.android.app.presentation.notification

import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.notification.NotificationViewModel
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.SchedMeetingNotification
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.usecase.AcknowledgeUserAlerts
import mega.privacy.android.domain.usecase.MonitorUserAlerts
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    private lateinit var underTest: NotificationViewModel

    private val monitorUserAlerts = mock<MonitorUserAlerts> {
        onBlocking { invoke() }.thenReturn(
            emptyFlow())
    }

    private val acknowledgeUserAlerts = mock<AcknowledgeUserAlerts>()

    private val scheduler = TestCoroutineScheduler()

    private val notificationMapper = mock<(UserAlert) -> Notification>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = NotificationViewModel(
            monitorUserAlerts = monitorUserAlerts,
            acknowledgeUserAlerts = acknowledgeUserAlerts,
            notificationMapper = notificationMapper,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
            titleTextSize = 16.dp,
            titleMaxWidth = { 200 },
            description = { "" },
            descriptionMaxWidth = { 300 },
            schedMeetingNotification = null,
            dateText = { "" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}

        val alert = mock<UserAlert>()
        whenever(monitorUserAlerts()).thenReturn(flowOf(listOf(alert)))
        whenever(notificationMapper(alert)).thenReturn(expected)


        underTest.state.drop(1).test {
            val (notifications, _) = awaitItem()
            assertWithMessage("Expected returned user alerts").that(notifications)
                .containsExactly(expected)
        }
    }

    @Test
    fun `test that should scroll is updated to true if new items appear`() = runTest {
        val initialAlert = mock<UserAlert>()
        val newAlert = mock<UserAlert>()
        val initialNotification = Notification(
            sectionTitle = { "" },
            sectionColour = 0,
            sectionIcon = null,
            title = { "Initial" },
            titleTextSize = 16.dp,
            titleMaxWidth = { 200 },
            description = { "" },
            descriptionMaxWidth = { 300 },
            schedMeetingNotification = null,
            dateText = { "" },
            isNew = true,
            backgroundColor = { "#D3D3D3" },
            separatorMargin = { 0 },
        ) {}
        val newNotification = initialNotification.copy(title = {"New title"})
        whenever(notificationMapper(initialAlert)).thenReturn(initialNotification)
        whenever(notificationMapper(newAlert)).thenReturn(newNotification)

        whenever(monitorUserAlerts()).thenReturn(
            flowOf(
                listOf(initialAlert),
                listOf(newAlert, initialAlert)
            )
        )

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
        verifyBlocking(acknowledgeUserAlerts, AcknowledgeUserAlerts::invoke)
    }
}