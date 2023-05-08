package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.repository.NotificationsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorUserAlertsTest {
    private lateinit var underTest: MonitorUserAlerts
    private val notificationsRepository = mock<NotificationsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorUserAlerts(notificationsRepository = notificationsRepository)
    }

    @Test
    fun `test that current items are returned`() = runTest {
        val expected = listOf(mock<IncomingPendingContactRequestAlert>())
        whenever(notificationsRepository.getUserAlerts()).thenReturn(expected)
        whenever(notificationsRepository.monitorUserAlerts()).thenReturn(flowOf())

        underTest().test {
            assertThat(awaitItem()).containsExactlyElementsIn(expected)
            awaitComplete()
        }
    }

    @Test
    fun `test that new events that already exist replace the existing alert`() = runTest {
        val alert1 = mock<IncomingPendingContactRequestAlert> {
            on { id }.thenReturn(1L)
        }
        val alert2 = mock<IncomingPendingContactRequestAlert> { on { id }.thenReturn(2L) }

        val alert1Update = mock<IncomingPendingContactRequestAlert> {
            on { id }.thenReturn(1L)
        }

        whenever(notificationsRepository.getUserAlerts()).thenReturn(listOf(alert1, alert2))
        whenever(notificationsRepository.monitorUserAlerts()).thenReturn(flowOf(listOf(alert1Update)))

        underTest().test {
            assertThat(awaitItem()).containsExactly(alert1, alert2)
            assertThat(awaitItem()).containsExactly(alert1Update, alert2)
            awaitComplete()
        }
    }

    @Test
    fun `test that subsequent updates retain previous updates`() = runTest {
        val alert1 = mock<IncomingPendingContactRequestAlert> {
            on { id }.thenReturn(1L)
        }
        val alert2 = mock<IncomingPendingContactRequestAlert> { on { id }.thenReturn(2L) }

        val alert1Update = mock<IncomingPendingContactRequestAlert> {
            on { id }.thenReturn(1L)
        }

        val alert2Update = mock<IncomingPendingContactRequestAlert> { on { id }.thenReturn(2L) }

        whenever(notificationsRepository.getUserAlerts()).thenReturn(listOf(alert1, alert2))
        whenever(notificationsRepository.monitorUserAlerts()).thenReturn(flowOf(listOf(alert1Update),
            listOf(alert2Update)))

        underTest().test {
            assertThat(awaitItem()).containsExactly(alert1, alert2)
            assertThat(awaitItem()).containsExactly(alert1Update, alert2)
            assertThat(awaitItem()).containsExactly(alert1Update, alert2Update)
            awaitComplete()
        }
    }

    @Test
    fun `test that events are returned in reverse chronological order`() = runTest {
        val alerts = (1L..5L).map { value ->
            mock<IncomingPendingContactRequestAlert> {
                on { id }.thenReturn(value)
                on { createdTime }.thenReturn(
                    value
                )
            }
        }

        whenever(notificationsRepository.getUserAlerts()).thenReturn(alerts)
        whenever(notificationsRepository.monitorUserAlerts()).thenReturn(flowOf())

        underTest().test {
            assertThat(awaitItem()).isInOrder(Comparator<UserAlert> { t, t2 ->
                t.createdTime.compareTo(t2.createdTime)
            }.reversed())
            awaitComplete()
        }
    }

    @Test
    fun `test that own changed updates are ignored`() = runTest {
        val alert1 = mock<IncomingPendingContactRequestAlert> {
            on { id }.thenReturn(1L)
        }
        val alert2 = mock<IncomingPendingContactRequestAlert> { on { id }.thenReturn(2L) }

        val alert1Update = mock<IncomingPendingContactRequestAlert> {
            on { id }.thenReturn(1L)
            on { isOwnChange }.thenReturn(true)
        }

        whenever(notificationsRepository.getUserAlerts()).thenReturn(listOf(alert1, alert2))
        whenever(notificationsRepository.monitorUserAlerts()).thenReturn(flowOf(listOf(alert1Update)))

        underTest().test {
            assertThat(awaitItem()).containsExactly(alert1, alert2)
            awaitComplete()
        }
    }
}