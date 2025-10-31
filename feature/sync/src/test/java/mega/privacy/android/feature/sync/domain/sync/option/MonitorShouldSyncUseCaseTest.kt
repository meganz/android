package mega.privacy.android.feature.sync.domain.sync.option

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorShouldSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorShouldSyncUseCaseTest {

    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()

    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()

    private val underTest = MonitorShouldSyncUseCase(
        monitorSyncByWiFiUseCase = monitorSyncByWiFiUseCase,
        monitorSyncByChargingUseCase = monitorSyncByChargingUseCase,
        monitorBatteryInfoUseCase = monitorBatteryInfoUseCase,
        isOnWifiNetworkUseCase = isOnWifiNetworkUseCase,
        monitorConnectivityUseCase = monitorConnectivityUseCase
    )

    @AfterEach
    fun resetMocks() {
        reset(
            monitorSyncByWiFiUseCase,
            monitorSyncByChargingUseCase,
            monitorBatteryInfoUseCase,
            isOnWifiNetworkUseCase,
            monitorConnectivityUseCase
        )
    }

    @Test
    fun `test that sync is allowed when all conditions are met`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync is not allowed when WiFi only is enabled but user is not on WiFi`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = true,
                chargingOnly = false,
                isOnWiFi = false
            )

            underTest().test {
                val result = awaitItem()
                assertThat(result).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sync is allowed when WiFi only is enabled and user is on WiFi`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = true,
            chargingOnly = false,
            isOnWiFi = true
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync is not allowed when charging only is enabled but device is not charging`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = false,
                chargingOnly = true,
                isOnWiFi = true
            )

            underTest().test {
                val result = awaitItem()
                assertThat(result).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sync is allowed when charging only is enabled and device is charging`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = true),
                wiFiOnly = false,
                chargingOnly = true,
                isOnWiFi = true
            )

            underTest().test {
                val result = awaitItem()
                assertThat(result).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that sync is not allowed when battery level is low and not charging`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 15, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync is allowed when battery level is low but device is charging`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 15, isCharging = true),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync is allowed when battery level is at threshold`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 20, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync is not allowed when isOnWifiNetworkUseCase throws exception`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = true,
            chargingOnly = true,
            isOnWiFi = false
        )
        whenever(isOnWifiNetworkUseCase()).thenThrow(RuntimeException("Network error"))

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync is not allowed when all restrictive conditions are enabled`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 15, isCharging = false),
            wiFiOnly = true,
            chargingOnly = true,
            isOnWiFi = false
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync is allowed when all restrictive conditions are met`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = true),
            wiFiOnly = true,
            chargingOnly = true,
            isOnWiFi = true
        )

        underTest().test {
            val result = awaitItem()
            assertThat(result).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sync monitors connectivity changes`() = runTest {
        val connectivityFlow = MutableSharedFlow<Boolean>()
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true,
            connectivityFlow = connectivityFlow
        )

        underTest().test {
            // Initial connectivity state
            connectivityFlow.emit(true)
            val firstResult = awaitItem()
            assertThat(firstResult).isTrue()

            // Connectivity change
            connectivityFlow.emit(false)
            val secondResult = awaitItem()
            assertThat(secondResult).isTrue() // Should still be true as we only monitor changes

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that connectivity changes are sampled`() = runTest {
        val connectivityFlow = MutableSharedFlow<Boolean>()
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true,
            connectivityFlow = connectivityFlow
        )

        underTest().test {
            // Rapid connectivity changes
            connectivityFlow.emit(true)
            connectivityFlow.emit(false)
            connectivityFlow.emit(true)

            // Advance time by less than sample period - should not emit
            advanceTimeBy(500) // 0.5 seconds
            expectNoEvents()

            // Advance time to complete the sample period
            advanceTimeBy(500) // Total 1 second
            val result = awaitItem()
            assertThat(result).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }


    private fun setupMocks(
        batteryInfo: BatteryInfo,
        wiFiOnly: Boolean,
        chargingOnly: Boolean,
        isOnWiFi: Boolean,
        connectivityFlow: Flow<Boolean>? = null,
    ) {
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(wiFiOnly))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(chargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(isOnWiFi)
        whenever(monitorConnectivityUseCase()).thenReturn(
            connectivityFlow ?: flowOf(true).onCompletion {
                awaitCancellation()
            })
    }
}
