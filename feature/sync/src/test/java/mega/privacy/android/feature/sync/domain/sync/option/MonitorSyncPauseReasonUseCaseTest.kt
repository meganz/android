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
import mega.privacy.android.domain.usecase.environment.MonitorPowerSaveModeUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.entity.SyncPauseReason
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorPauseSyncOnBatterySaverUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncPauseReasonUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncPauseReasonUseCaseTest {

    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase = mock()
    private val monitorPauseSyncOnBatterySaverUseCase: MonitorPauseSyncOnBatterySaverUseCase =
        mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val monitorPowerSaveModeUseCase: MonitorPowerSaveModeUseCase = mock()
    private val isOnWifiNetworkUseCase: IsOnWifiNetworkUseCase = mock()

    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock()

    private val underTest = MonitorSyncPauseReasonUseCase(
        monitorSyncByWiFiUseCase = monitorSyncByWiFiUseCase,
        monitorSyncByChargingUseCase = monitorSyncByChargingUseCase,
        monitorPauseSyncOnBatterySaverUseCase = monitorPauseSyncOnBatterySaverUseCase,
        monitorBatteryInfoUseCase = monitorBatteryInfoUseCase,
        monitorPowerSaveModeUseCase = monitorPowerSaveModeUseCase,
        isOnWifiNetworkUseCase = isOnWifiNetworkUseCase,
        monitorConnectivityUseCase = monitorConnectivityUseCase
    )

    @AfterEach
    fun resetMocks() {
        reset(
            monitorSyncByWiFiUseCase,
            monitorSyncByChargingUseCase,
            monitorPauseSyncOnBatterySaverUseCase,
            monitorBatteryInfoUseCase,
            monitorPowerSaveModeUseCase,
            isOnWifiNetworkUseCase,
            monitorConnectivityUseCase
        )
    }

    @Test
    fun `test that no reason is emitted when all conditions are met`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true
        )

        assertReason(null)
    }

    @Test
    fun `test that no wifi is emitted when WiFi only is enabled but user is not on WiFi`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = true,
                chargingOnly = false,
                isOnWiFi = false
            )

            assertReason(SyncPauseReason.NoWifi)
        }

    @Test
    fun `test that no reason is emitted when WiFi only is enabled and user is on WiFi`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = true,
            chargingOnly = false,
            isOnWiFi = true
        )

        assertReason(null)
    }

    @Test
    fun `test that not charging is emitted when charging only is enabled but device is not charging`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = false,
                chargingOnly = true,
                isOnWiFi = true
            )

            assertReason(SyncPauseReason.NotCharging)
        }

    @Test
    fun `test that no reason is emitted when charging only is enabled and device is charging`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = true),
                wiFiOnly = false,
                chargingOnly = true,
                isOnWiFi = true
            )

            assertReason(null)
        }

    @Test
    fun `test that low battery is emitted when battery level is low and not charging`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 15, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true
        )

        assertReason(SyncPauseReason.LowBattery)
    }

    @Test
    fun `test that no reason is emitted when battery level is low but device is charging`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 15, isCharging = true),
                wiFiOnly = false,
                chargingOnly = false,
                isOnWiFi = true
            )

            assertReason(null)
        }

    @Test
    fun `test that no reason is emitted when battery level is at threshold`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 20, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true
        )

        assertReason(null)
    }

    @Test
    fun `test that battery saver is emitted when pause on battery saver is enabled and device is in power save mode`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = false,
                chargingOnly = false,
                isOnWiFi = true,
                pauseOnBatterySaver = true,
                isInPowerSaveMode = true
            )

            assertReason(SyncPauseReason.BatterySaver)
        }

    @Test
    fun `test that no reason is emitted when pause on battery saver is enabled but device is not in power save mode`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = false,
                chargingOnly = false,
                isOnWiFi = true,
                pauseOnBatterySaver = true,
                isInPowerSaveMode = false
            )

            assertReason(null)
        }

    @Test
    fun `test that no reason is emitted when device is in power save mode but pause on battery saver is disabled`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = false,
                chargingOnly = false,
                isOnWiFi = true,
                pauseOnBatterySaver = false,
                isInPowerSaveMode = true
            )

            assertReason(null)
        }

    @Test
    fun `test that no reason is emitted when device is in power save mode but is charging`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = true),
                wiFiOnly = false,
                chargingOnly = false,
                isOnWiFi = true,
                pauseOnBatterySaver = true,
                isInPowerSaveMode = true
            )

            assertReason(null)
        }

    @Test
    fun `test that no reason is emitted when charging only and pause on battery saver are both enabled and device is charging in power save mode`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = true),
                wiFiOnly = false,
                chargingOnly = true,
                isOnWiFi = true,
                pauseOnBatterySaver = true,
                isInPowerSaveMode = true
            )

            assertReason(null)
        }

    @Test
    fun `test that battery saver takes precedence over not charging when both apply`() =
        runTest {
            setupMocks(
                batteryInfo = BatteryInfo(level = 50, isCharging = false),
                wiFiOnly = false,
                chargingOnly = true,
                isOnWiFi = true,
                pauseOnBatterySaver = true,
                isInPowerSaveMode = true
            )

            assertReason(SyncPauseReason.BatterySaver)
        }

    @Test
    fun `test that low battery takes precedence over battery saver when both apply`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 15, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true,
            pauseOnBatterySaver = true,
            isInPowerSaveMode = true
        )

        assertReason(SyncPauseReason.LowBattery)
    }

    @Test
    fun `test that no wifi takes precedence over not charging when both apply`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = true,
            chargingOnly = true,
            isOnWiFi = false
        )

        assertReason(SyncPauseReason.NoWifi)
    }

    @Test
    fun `test that no wifi is emitted when isOnWifiNetworkUseCase throws exception`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = true,
            chargingOnly = true,
            isOnWiFi = false
        )
        whenever(isOnWifiNetworkUseCase()).thenThrow(RuntimeException("Network error"))

        assertReason(SyncPauseReason.NoWifi)
    }

    @Test
    fun `test that low battery is emitted when all restrictive conditions are enabled`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 15, isCharging = false),
            wiFiOnly = true,
            chargingOnly = true,
            isOnWiFi = false
        )

        assertReason(SyncPauseReason.LowBattery)
    }

    @Test
    fun `test that no reason is emitted when all restrictive conditions are met`() = runTest {
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = true),
            wiFiOnly = true,
            chargingOnly = true,
            isOnWiFi = true
        )

        assertReason(null)
    }

    @Test
    fun `test that connectivity changes are monitored`() = runTest {
        val connectivityFlow = MutableSharedFlow<Boolean>()
        setupMocks(
            batteryInfo = BatteryInfo(level = 50, isCharging = false),
            wiFiOnly = false,
            chargingOnly = false,
            isOnWiFi = true,
            connectivityFlow = connectivityFlow
        )

        underTest().test {
            connectivityFlow.emit(true)
            assertThat(awaitItem()).isNull()

            // Should still be allowed as we only monitor changes
            connectivityFlow.emit(false)
            assertThat(awaitItem()).isNull()

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
            connectivityFlow.emit(true)
            connectivityFlow.emit(false)
            connectivityFlow.emit(true)

            advanceTimeBy(500)
            expectNoEvents()

            advanceTimeBy(500)
            assertThat(awaitItem()).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun assertReason(expected: SyncPauseReason?) {
        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun setupMocks(
        batteryInfo: BatteryInfo,
        wiFiOnly: Boolean,
        chargingOnly: Boolean,
        isOnWiFi: Boolean,
        pauseOnBatterySaver: Boolean = false,
        isInPowerSaveMode: Boolean = false,
        connectivityFlow: Flow<Boolean>? = null,
    ) {
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(batteryInfo))
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(wiFiOnly))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(chargingOnly))
        whenever(monitorPauseSyncOnBatterySaverUseCase()).thenReturn(flowOf(pauseOnBatterySaver))
        whenever(monitorPowerSaveModeUseCase()).thenReturn(flowOf(isInPowerSaveMode))
        wheneverBlocking { isOnWifiNetworkUseCase() }.thenReturn(isOnWiFi)
        whenever(monitorConnectivityUseCase()).thenReturn(
            connectivityFlow ?: flowOf(true).onCompletion {
                awaitCancellation()
            })
    }
}
