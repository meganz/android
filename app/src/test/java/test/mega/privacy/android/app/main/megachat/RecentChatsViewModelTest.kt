package test.mega.privacy.android.app.main.megachat

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.chat.recent.RecentChatsViewModel
import mega.privacy.android.app.presentation.chat.recent.RecentChatsViewModel.Companion.DURATION_TO_SHOW_REQUEST_ACCESS_AGAIN
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.domain.usecase.GetLastContactPermissionDismissedTime
import mega.privacy.android.domain.usecase.SetLastContactPermissionDismissedTime
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
internal class RecentChatsViewModelTest {
    private lateinit var underTest: RecentChatsViewModel

    private val setLastContactPermissionDismissedTime =
        mock<SetLastContactPermissionDismissedTime>()
    private val getLastContactPermissionDismissedTime =
        mock<GetLastContactPermissionDismissedTime>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val getDeviceCurrentTimeUseCase = mock<GetDeviceCurrentTimeUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test last dismissed time and current time equal`() = runTest {
        whenever(getDeviceCurrentTimeUseCase()).thenReturn(0L)
        whenever(getLastContactPermissionDismissedTime()).thenReturn(flowOf(0L))
        underTest = RecentChatsViewModel(
            setLastContactPermissionDismissedTime = setLastContactPermissionDismissedTime,
            getLastContactPermissionDismissedTime = getLastContactPermissionDismissedTime,
            ioDispatcher = UnconfinedTestDispatcher(),
            getDeviceCurrentTimeUseCase = getDeviceCurrentTimeUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
        )

        underTest.state.test {
            val state = awaitItem()
            assertEquals(state.shouldShowRequestContactAccess, false)
        }
    }

    @Test
    fun `test that when last dismissed time is less than DURATION_TO_SHOW_REQUEST_ACCESS_AGAIN after current time, then shouldShowRequestContactAccess is false`() =
        runTest {
            whenever(getDeviceCurrentTimeUseCase()).thenReturn(
                DURATION_TO_SHOW_REQUEST_ACCESS_AGAIN - 1
            )
            whenever(getLastContactPermissionDismissedTime()).thenReturn(flowOf(0L))
            underTest = RecentChatsViewModel(
                setLastContactPermissionDismissedTime = setLastContactPermissionDismissedTime,
                getLastContactPermissionDismissedTime = getLastContactPermissionDismissedTime,
                ioDispatcher = UnconfinedTestDispatcher(),
                getDeviceCurrentTimeUseCase = getDeviceCurrentTimeUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            )

            underTest.state.test {
                val state = awaitItem()
                assertEquals(state.shouldShowRequestContactAccess, false)
            }
        }

    @Test
    fun `test that when last dismissed time is more than DURATION_TO_SHOW_REQUEST_ACCESS_AGAIN after current time, then shouldShowRequestContactAccess is true`() =
        runTest {
            whenever(getDeviceCurrentTimeUseCase()).thenReturn(
                DURATION_TO_SHOW_REQUEST_ACCESS_AGAIN + 1
            )
            whenever(getLastContactPermissionDismissedTime()).thenReturn(flowOf(0L))
            underTest = RecentChatsViewModel(
                setLastContactPermissionDismissedTime = setLastContactPermissionDismissedTime,
                getLastContactPermissionDismissedTime = getLastContactPermissionDismissedTime,
                ioDispatcher = UnconfinedTestDispatcher(),
                getDeviceCurrentTimeUseCase = getDeviceCurrentTimeUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            )

            underTest.state.test {
                val state = awaitItem()
                assertEquals(state.shouldShowRequestContactAccess, true)
            }
        }
}
