package mega.privacy.android.domain.usecase.environment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.EnvironmentRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MonitorPowerSaveModeUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorPowerSaveModeUseCaseTest {

    private lateinit var underTest: MonitorPowerSaveModeUseCase

    private val environmentRepository = mock<EnvironmentRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorPowerSaveModeUseCase(environmentRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(environmentRepository)
    }

    @ParameterizedTest(name = "power save mode enabled: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the power save mode state is returned`(isEnabled: Boolean) = runTest {
        whenever(environmentRepository.monitorPowerSaveMode()).thenReturn(flowOf(isEnabled))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(isEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
