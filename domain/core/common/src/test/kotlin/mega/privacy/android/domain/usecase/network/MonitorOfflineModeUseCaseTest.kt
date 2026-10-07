package mega.privacy.android.domain.usecase.network

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOfflineModeUseCaseTest {
    private lateinit var underTest: MonitorOfflineModeUseCase

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorOfflineModeUseCase(
            monitorConnectivityUseCase = monitorConnectivityUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorConnectivityUseCase)
    }

    @Test
    fun `test that invoke emits true when disconnected`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(false))

        underTest().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits false when connected`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true))

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits only when offline mode changes`() = runTest {
        whenever(monitorConnectivityUseCase()).thenReturn(flowOf(true, true, false, false, true))

        underTest().test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }
}
