package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.usecase.sync.ClearSyncDebrisUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetSyncDebrisSizeInBytesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncAction.SyncOptionSelected
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncAction.SnackbarShown
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncAction.ClearDebrisClicked
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncViewModel
import mega.privacy.android.shared.resources.R
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsSyncViewModelTest {

    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase = mock()
    private val getSyncDebrisSizeUseCase: GetSyncDebrisSizeInBytesUseCase = mock()
    private val clearSyncDebrisUseCase: ClearSyncDebrisUseCase = mock()

    private lateinit var underTest: SettingsSyncViewModel

    @BeforeEach
    fun setup() {
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf())
        getSyncDebrisSizeUseCase.stub {
            onBlocking { invoke() }.thenReturn(0L)
        }
    }

    @AfterEach
    fun tearDown() {
        reset(monitorSyncByWiFiUseCase, setSyncByWiFiUseCase)
    }

    @Test
    fun `test that setSyncByWiFiUseCase is set to true when wifi only is selected`() = runTest {
        initViewModel()

        underTest.handleAction(SyncOptionSelected(SyncOption.WI_FI_ONLY))

        verify(setSyncByWiFiUseCase).invoke(true)
    }

    @Test
    fun `test that setSyncByWiFiUseCase is set to false when wifi or mobile data is selected`() =
        runTest {
            initViewModel()

            underTest.handleAction(SyncOptionSelected(SyncOption.WI_FI_OR_MOBILE_DATA))

            verify(setSyncByWiFiUseCase).invoke(false)
        }

    @Test
    fun `test that when monitorSyncsByWiFiUseCase emits true state is changed to wifi only`() =
        runTest {
            whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(true))
            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().syncOption).isEqualTo(SyncOption.WI_FI_ONLY)
            }
        }

    @Test
    fun `test that when monitorSyncsByWiFiUseCase emits false state is changed to wifi or mobile data`() =
        runTest {
            whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf(false))
            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().syncOption).isEqualTo(SyncOption.WI_FI_OR_MOBILE_DATA)
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
        underTest.handleAction(ClearDebrisClicked)

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(R.string.settings_sync_debris_cleared_message)
        }
    }

    @Test
    fun `test that after snackbar is shown its state is reset`() = runTest {
        initViewModel()

        underTest.handleAction(SnackbarShown)

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(null)
        }
    }

    private fun initViewModel() {
        underTest = SettingsSyncViewModel(
            monitorSyncByWiFiUseCase,
            setSyncByWiFiUseCase,
            getSyncDebrisSizeUseCase,
            clearSyncDebrisUseCase
        )
    }
}