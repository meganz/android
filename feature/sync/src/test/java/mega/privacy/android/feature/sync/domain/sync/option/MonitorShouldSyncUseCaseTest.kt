package mega.privacy.android.feature.sync.domain.sync.option

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.usecase.IsOnWifiNetworkUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
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
class MonitorShouldSyncUseCaseTest {

    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()

    private val underTest = MonitorShouldSyncUseCase(
        monitorSyncByWiFiUseCase = monitorSyncByWiFiUseCase,
        monitorSyncByChargingUseCase = monitorSyncByChargingUseCase,
        monitorBatteryInfoUseCase = monitorBatteryInfoUseCase,
        isOnWifiNetworkUseCase = isOnWifiNetworkUseCase,
    )

    @AfterEach
    fun resetMocks() {
        reset(
            monitorSyncByWiFiUseCase,
            monitorSyncByChargingUseCase,
            monitorBatteryInfoUseCase,
            isOnWifiNetworkUseCase
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

    private fun setupMocks(
        batteryInfo: BatteryInfo,
        wiFiOnly: Boolean,
        chargingOnly: Boolean,
        isOnWiFi: Boolean,
    ) {
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(wiFiOnly))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(chargingOnly))
        whenever(isOnWifiNetworkUseCase()).thenReturn(isOnWiFi)
    }
}
