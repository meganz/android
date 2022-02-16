package test.mega.privacy.android.app.presentation.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.HasIncomingCall
import mega.privacy.android.app.domain.usecase.MonitorChatNotificationCount
import mega.privacy.android.app.presentation.home.HomeViewModel
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HomeViewModelTest {
    private lateinit var underTest: HomeViewModel

    private val monitorChatNotificationCount = mock<MonitorChatNotificationCount> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val hasIncomingCall = mock<HasIncomingCall>{
        on { invoke() }.thenReturn(emptyFlow())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = HomeViewModel(
            monitorChatNotificationCount = monitorChatNotificationCount,
            hasIncomingCall = hasIncomingCall,
            )
    }

    @Test
    fun `test that initial unread count is 0`() {
        assertThat(underTest.homeState.value.unreadNotificationsCount).isEqualTo(0)
    }

    @Test
    fun `test that display badge is initially set to false`() {
        assertThat(underTest.homeState.value.displayChatCount).isFalse()
    }

    @Test
    fun `test that new unread notifications update the count`() = runTest {
        val expectedCount = 5
        whenever(monitorChatNotificationCount()).thenReturn(flowOf(expectedCount))
        whenever(hasIncomingCall()).thenReturn(flowOf(false))

        underTest.homeState.test {
            awaitItem()
            assertThat(awaitItem().unreadNotificationsCount).isEqualTo(expectedCount)
        }
    }

    @Test
    fun `test that new unread notifications sets display badge to true`() = runTest{
        whenever(monitorChatNotificationCount()).thenReturn(flowOf(5))
        whenever(hasIncomingCall()).thenReturn(flowOf(false))

        underTest.homeState.test {
            awaitItem()
            assertThat(awaitItem().displayChatCount).isTrue()
        }
    }

    @Test
    fun `tet that display call badge is initially false`() {
        assertThat(underTest.homeState.value.displayCallBadge).isFalse()
    }

    @Test
    fun `test that display call badge is true when an incoming call is present`() = runTest{
        whenever(hasIncomingCall()).thenReturn(flowOf(true))
        whenever(monitorChatNotificationCount()).thenReturn(flowOf(0))

        underTest.homeState.test {
            awaitItem()
            assertThat(awaitItem().displayCallBadge).isTrue()
        }
    }

    @Test
    fun `test that display unread notifications is set to false when an incoming call is present`() = runTest{
        whenever(monitorChatNotificationCount()).thenReturn(flowOf(5))
        whenever(hasIncomingCall()).thenReturn(flowOf(true))

        underTest.homeState.test {
            awaitItem()
            assertThat(awaitItem().displayChatCount).isFalse()
        }
    }

    @Test
    fun `test that notification badges are re-enabled once a call is done`() = runTest{
        whenever(monitorChatNotificationCount()).thenReturn(flowOf(5))
        whenever(hasIncomingCall()).thenReturn(flowOf(true, false))

        underTest.homeState.test {
            awaitItem()
            assertThat(awaitItem().displayChatCount).isFalse()
            assertThat(awaitItem().displayChatCount).isTrue()
        }
    }
}