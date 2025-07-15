package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.R as syncR
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.ClearSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.MonitorSyncSolvedIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ClearSyncDebrisUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncDebrisSizeInBytesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByChargingUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.GetSyncFrequencyUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.worker.SetSyncFrequencyUseCase
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncAction
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncViewModel
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SettingsSyncViewModelTest {

    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val monitorSyncByChargingUseCase: MonitorSyncByChargingUseCase = mock()
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase = mock()
    private val getSyncDebrisSizeUseCase: GetSyncDebrisSizeInBytesUseCase = mock()
    private val clearSyncDebrisUseCase: ClearSyncDebrisUseCase = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getSyncFrequencyUseCase: GetSyncFrequencyUseCase = mock()
    private val setSyncFrequencyUseCase: SetSyncFrequencyUseCase = mock()
    private val setSyncByChargingUseCase = mock<SetSyncByChargingUseCase>()
    private val monitorSyncSolvedIssuesUseCase: MonitorSyncSolvedIssuesUseCase = mock()
    private val clearSyncSolvedIssuesUseCase: ClearSyncSolvedIssuesUseCase = mock()

    private lateinit var underTest: SettingsSyncViewModel

    @BeforeEach
    fun setup() {
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(false))
        whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(false))
        getSyncDebrisSizeUseCase.stub {
            onBlocking { invoke() }.thenReturn(0L)
        }
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorSyncByWiFiUseCase,
            monitorSyncByChargingUseCase,
            setSyncByWiFiUseCase,
            setSyncByChargingUseCase,
            getSyncDebrisSizeUseCase,
            clearSyncDebrisUseCase,
            getFeatureFlagValueUseCase,
            getSyncFrequencyUseCase,
            setSyncFrequencyUseCase,
            monitorSyncSolvedIssuesUseCase,
        )
    }

    @Test
    fun `test that setSyncByWiFiUseCase is set to true when wifi only is selected`() = runTest {
        initViewModel()

        underTest.handleAction(SettingsSyncAction.SyncConnectionTypeSelected(SyncConnectionType.WiFiOnly))

        verify(setSyncByWiFiUseCase).invoke(true)
    }

    @Test
    fun `test that setSyncByWiFiUseCase is set to false when wifi or mobile data is selected`() =
        runTest {
            initViewModel()

            underTest.handleAction(SettingsSyncAction.SyncConnectionTypeSelected(SyncConnectionType.WiFiOrMobileData))

            verify(setSyncByWiFiUseCase).invoke(false)
        }

    @Test
    fun `test that setSyncByChargingUseCase is set to true when sync only when charging is selected`() =
        runTest {
            initViewModel()

            underTest.handleAction(SettingsSyncAction.SyncPowerOptionSelected(SyncPowerOption.SyncOnlyWhenCharging))

            verify(setSyncByChargingUseCase).invoke(true)
        }

    @Test
    fun `test that setSyncByChargingUseCase is set to false when sync always is selected`() =
        runTest {
            initViewModel()

            underTest.handleAction(SettingsSyncAction.SyncPowerOptionSelected(SyncPowerOption.SyncAlways))

            verify(setSyncByChargingUseCase).invoke(false)
        }

    @Test
    fun `test that when monitorSyncByWiFiUseCase emits wifi only state is changed to wifi only`() =
        runTest {
            whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(true))
            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().syncConnectionType).isEqualTo(SyncConnectionType.WiFiOnly)
            }
        }

    @Test
    fun `test that when monitorSyncByWiFiUseCase emits wifi or mobile data state is changed accordingly`() =
        runTest {
            whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(false))
            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().syncConnectionType).isEqualTo(SyncConnectionType.WiFiOrMobileData)
            }
        }

    @Test
    fun `test that when monitorSyncByChargingUseCase emits sync only when charging state is changed accordingly`() =
        runTest {
            whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(true))
            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().syncPowerOption).isEqualTo(SyncPowerOption.SyncOnlyWhenCharging)
            }
        }

    @Test
    fun `test that when monitorSyncByChargingUseCase emits sync always state is changed accordingly`() =
        runTest {
            whenever(monitorSyncByChargingUseCase()).thenReturn(flowOf(false))
            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().syncPowerOption).isEqualTo(SyncPowerOption.SyncAlways)
            }
        }

    @Test
    fun `test that the debris size is loaded upon viewmodel initialization`() = runTest {
        val debrisSize = 23L
        whenever(getSyncDebrisSizeUseCase()).thenReturn(debrisSize)

        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().syncDebrisSizeInBytes).isEqualTo(debrisSize)
        }
    }

    @Test
    fun `test that after clearing debris snackbar is shown`() = runTest {
        whenever(clearSyncDebrisUseCase()).thenReturn(Unit)

        initViewModel()
        underTest.handleAction(SettingsSyncAction.ClearDebrisClicked)

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(listOf(sharedR.string.settings_sync_debris_cleared_message))
        }
    }

    @Test
    fun `test that after snackbar is shown its state is reset`() = runTest {
        initViewModel()

        underTest.handleAction(SettingsSyncAction.SnackbarShown)

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(null)
        }
    }

    @Test
    fun `test that enabled SyncFrequencySettings feature flag shows frequency upon view model init`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(SyncFeatures.SyncFrequencySettings)).thenReturn(true)
            whenever(getSyncFrequencyUseCase()).thenReturn(15)

            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showSyncFrequency).isEqualTo(true)
                assertThat(state.syncFrequency).isEqualTo(SyncFrequency.EVERY_15_MINUTES)
            }
        }

    @Test
    fun `test that disabled SyncFrequencySettings feature flag does not shows frequency`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(SyncFeatures.SyncFrequencySettings)).thenReturn(
                false
            )

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().showSyncFrequency).isEqualTo(false)
            }
        }

    @Test
    fun `test that set frequency selected updates ui state and invokes set frequency use case`() =
        runTest {
            val frequency = SyncFrequency.EVERY_30_MINUTES
            initViewModel()

            underTest.handleAction(SettingsSyncAction.SyncFrequencySelected(frequency))

            underTest.uiState.test {
                assertThat(awaitItem().syncFrequency).isEqualTo(frequency)
            }
            verify(setSyncFrequencyUseCase).invoke(frequency.minutes)
        }

    @ParameterizedTest
    @EnumSource(SyncConnectionType::class)
    fun `test that after changing sync network option snackbar is shown`(option: SyncConnectionType) =
        runTest {
            initViewModel()
            underTest.handleAction(SettingsSyncAction.SyncConnectionTypeSelected(option))
            val stringRes = when (option) {
                SyncConnectionType.WiFiOnly -> syncR.string.sync_dialog_message_wifi_only
                SyncConnectionType.WiFiOrMobileData -> syncR.string.sync_dialog_message_wifi_or_mobile_data
            }

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(
                    listOf(
                        sharedR.string.settings_sync_message_prefix,
                        stringRes
                    )
                )
            }
        }

    @ParameterizedTest
    @EnumSource(SyncPowerOption::class)
    fun `test that after changing sync power option snackbar is shown`(option: SyncPowerOption) =
        runTest {
            initViewModel()
            val stringRes = when (option) {
                SyncPowerOption.SyncAlways -> sharedR.string.settings_sync_power_always_title
                SyncPowerOption.SyncOnlyWhenCharging -> sharedR.string.settings_sync_battery_sync_only_when_charging_title
            }
            underTest.handleAction(SettingsSyncAction.SyncPowerOptionSelected(option))

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(
                    listOf(
                        sharedR.string.settings_sync_message_prefix,
                        stringRes
                    )
                )
            }
        }

    @Test
    fun `test that after changing sync frequency snackbar is shown`() = runTest {
        initViewModel()
        underTest.handleAction(SettingsSyncAction.SyncFrequencySelected(SyncFrequency.EVERY_30_MINUTES))

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(listOf(sharedR.string.settings_sync_option_updated_message))
        }
    }

    @Test
    fun `test that shouldShowCleanSolvedIssueMenuItem is true when monitorSyncSolvedIssuesUseCase emits non-empty list`() =
        runTest {
            val solvedIssues = listOf(mock<SolvedIssue>()) // Fixed: actual non-empty list
            whenever(monitorSyncSolvedIssuesUseCase()).thenReturn(flowOf(solvedIssues))

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().shouldShowCleanSolvedIssueMenuItem).isTrue()
            }
        }

    @Test
    fun `test that clearSyncDebrisUseCase is called when ClearDebrisClicked action is handled`() =
        runTest {
            initViewModel()

            underTest.handleAction(SettingsSyncAction.ClearDebrisClicked)

            verify(clearSyncDebrisUseCase).invoke()
        }

    @Test
    fun `test that debris size is refreshed after clearing debris`() = runTest {
        val initialDebrisSize = 100L
        val clearedDebrisSize = 0L
        whenever(getSyncDebrisSizeUseCase()).thenReturn(initialDebrisSize, clearedDebrisSize)
        whenever(clearSyncDebrisUseCase()).thenReturn(Unit)

        initViewModel()
        underTest.handleAction(SettingsSyncAction.ClearDebrisClicked)

        verify(getSyncDebrisSizeUseCase, times(2)).invoke()
        verify(clearSyncDebrisUseCase).invoke()
    }

    private fun initViewModel() {
        underTest = SettingsSyncViewModel(
            monitorSyncByWiFiUseCase = monitorSyncByWiFiUseCase,
            monitorSyncByChargingUseCase = monitorSyncByChargingUseCase,
            setSyncByWiFiUseCase = setSyncByWiFiUseCase,
            setSyncByChargingUseCase = setSyncByChargingUseCase,
            getSyncDebrisSizeUseCase = getSyncDebrisSizeUseCase,
            clearSyncDebrisUseCase = clearSyncDebrisUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getSyncFrequencyUseCase = getSyncFrequencyUseCase,
            setSyncFrequencyUseCase = setSyncFrequencyUseCase,
            monitorSyncSolvedIssuesUseCase = monitorSyncSolvedIssuesUseCase,
            clearSyncSolvedIssuesUseCase = clearSyncSolvedIssuesUseCase
        )
    }
}
