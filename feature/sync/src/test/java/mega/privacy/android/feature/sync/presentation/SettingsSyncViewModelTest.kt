package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetSyncByWiFiUseCase
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.feature.sync.ui.settings.SettingsSyncViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsSyncViewModelTest {

    private val monitorSyncByWiFiUseCase: MonitorSyncByWiFiUseCase = mock()
    private val setSyncByWiFiUseCase: SetSyncByWiFiUseCase = mock()

    private lateinit var underTest: SettingsSyncViewModel

    @BeforeEach
    fun setup() {
        whenever(monitorSyncByWiFiUseCase()).thenReturn(flowOf())
    }

    @AfterEach
    fun tearDown() {
        reset(monitorSyncByWiFiUseCase, setSyncByWiFiUseCase)
    }

    @Test
    fun `test that setSyncByWiFiUseCase is set to true when wifi only is selected`() = runTest {
        initViewModel()

        underTest.setSyncByWiFi(SyncOption.WI_FI_ONLY)

        verify(setSyncByWiFiUseCase).invoke(true)
    }

    @Test
    fun `test that setSyncByWiFiUseCase is set to false when wifi or mobile data is selected`() =
        runTest {
            initViewModel()

            underTest.setSyncByWiFi(SyncOption.WI_FI_OR_MOBILE_DATA)

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

    private fun initViewModel() {
        underTest = SettingsSyncViewModel(monitorSyncByWiFiUseCase, setSyncByWiFiUseCase)
    }
}