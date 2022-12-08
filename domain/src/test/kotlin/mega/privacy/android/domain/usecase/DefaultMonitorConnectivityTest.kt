package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorConnectivityTest {
    private lateinit var underTest: MonitorConnectivity

    private val networkRepository = mock<NetworkRepository>()

    @Before
    fun setUp() {
        underTest = DefaultMonitorConnectivity(networkRepository = networkRepository,
            appScope = CoroutineScope(
                UnconfinedTestDispatcher()))
    }

    @Test
    fun `test that initial value is current connected state`() = runTest {
        networkRepository.stub {
            onBlocking { getCurrentConnectivityState() }.thenReturn(ConnectivityState.Connected(
                false))
            on { monitorConnectivityChanges() }.thenReturn(emptyFlow())
        }

        underTest().test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that subsequent states have their connected property returned`() = runTest {
        val connectivityFlow = MutableStateFlow(ConnectivityState.Disconnected)
        networkRepository.stub {
            onBlocking { getCurrentConnectivityState() }.thenReturn(ConnectivityState.Connected(
                false))
            on { monitorConnectivityChanges() }.thenReturn(connectivityFlow)
        }

        underTest().test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that connectivity state is updated if it changes`() = runTest {
        val connectivityFlow = MutableStateFlow<ConnectivityState>(ConnectivityState.Disconnected)
        networkRepository.stub {
            onBlocking { getCurrentConnectivityState() }.thenReturn(ConnectivityState.Connected(
                false))
            on { monitorConnectivityChanges() }.thenReturn(connectivityFlow)
        }

        underTest().test {
            assertThat(awaitItem()).isFalse()
            connectivityFlow.emit(ConnectivityState.Connected(false))
            assertThat(awaitItem()).isTrue()
        }
    }
}