package mega.privacy.android.domain.usecase.environment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorDevicePowerConnectionStateUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorDevicePowerConnectionStateUseCaseTest {

    private lateinit var underTest: MonitorDevicePowerConnectionStateUseCase

    private val environmentRepository = mock<EnvironmentRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorDevicePowerConnectionStateUseCase(environmentRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(environmentRepository)
    }

    @Test
    fun `test that the correct device power connection state is returned`() = runTest {
        val expectedState = DevicePowerConnectionState.Connected
        whenever(environmentRepository.monitorDevicePowerConnectionState()).thenReturn(
            flowOf(expectedState)
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expectedState)
            cancelAndIgnoreRemainingEvents()
        }
    }
}