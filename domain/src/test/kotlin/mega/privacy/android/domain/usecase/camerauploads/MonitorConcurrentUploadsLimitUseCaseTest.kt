package mega.privacy.android.domain.usecase.camerauploads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.environment.ThermalState
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.environment.MonitorDeviceThermalStateUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorConcurrentUploadsLimitUseCaseTest {
    private lateinit var underTest: MonitorConcurrentUploadsLimitUseCase

    private val monitorBatteryInfoUseCase = mock<MonitorBatteryInfoUseCase>()
    private val monitorDeviceThermalStateUseCase = mock<MonitorDeviceThermalStateUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorConcurrentUploadsLimitUseCase(
            monitorBatteryInfoUseCase = monitorBatteryInfoUseCase,
            monitorDeviceThermalStateUseCase = monitorDeviceThermalStateUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorBatteryInfoUseCase,
            monitorDeviceThermalStateUseCase,
        )
    }

    @ParameterizedTest(name = "when thermal state is {0}")
    @EnumSource(ThermalState::class)
    fun `test that the concurrent uploads limit adjust correctly based on thermal device`(
        thermalState: ThermalState
    ) = runTest {
        val defaultLimit = 8
        whenever(monitorDeviceThermalStateUseCase()).thenReturn(flowOf(thermalState))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(100, true)))

        val expected = when (thermalState) {
            ThermalState.ThermalStateNone, ThermalState.ThermalStateLight -> defaultLimit
            ThermalState.ThermalStateModerate -> defaultLimit / 2
            else -> 1
        }

        underTest(defaultLimit).test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "when battery level is {0}")
    @ValueSource(ints = [100, 80, 75, 60, 50, 30, 20])
    fun `test that the concurrent uploads limit adjust correctly based on battery level`(
        batteryLevel: Int
    ) = runTest {
        val defaultLimit = 8
        whenever(monitorDeviceThermalStateUseCase()).thenReturn(flowOf(ThermalState.ThermalStateNone))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(batteryLevel, false)))

        val expected = when {
            batteryLevel >= 50 -> defaultLimit
            else -> defaultLimit / 2
        }

        underTest(defaultLimit).test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest(name = "when device is charging {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the concurrent uploads limit adjust correctly based on charging state`(
        isCharging: Boolean
    ) = runTest {
        val defaultLimit = 8
        whenever(monitorDeviceThermalStateUseCase()).thenReturn(flowOf(ThermalState.ThermalStateNone))
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(49, isCharging)))

        val expected = when (isCharging) {
            true -> defaultLimit
            else -> defaultLimit / 2
        }

        underTest(defaultLimit).test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the concurrent uploads limit adjust correctly based on thermal device and battery level`() =
        runTest {
            val defaultLimit = 8
            whenever(monitorDeviceThermalStateUseCase()).thenReturn(flowOf(ThermalState.ThermalStateSevere))
            whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(25, false)))

            val expected = 1

            underTest(defaultLimit).test {
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
