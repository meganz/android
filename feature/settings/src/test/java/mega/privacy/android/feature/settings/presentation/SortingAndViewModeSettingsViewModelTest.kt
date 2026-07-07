package mega.privacy.android.feature.settings.presentation

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.usecase.setting.MonitorSortingPreferenceUseCase
import mega.privacy.android.domain.usecase.setting.MonitorViewModePreferenceUseCase
import mega.privacy.android.domain.usecase.setting.SetSortingPreferenceUseCase
import mega.privacy.android.domain.usecase.setting.SetViewModePreferenceUseCase
import mega.privacy.android.feature.settings.presentation.model.SortingAndViewModeSettingsUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
class SortingAndViewModeSettingsViewModelTest {
    private lateinit var underTest: SortingAndViewModeSettingsViewModel

    private val monitorSortingPreferenceUseCase = mock<MonitorSortingPreferenceUseCase>()
    private val setSortingPreferenceUseCase = mock<SetSortingPreferenceUseCase>()
    private val monitorViewModePreferenceUseCase = mock<MonitorViewModePreferenceUseCase>()
    private val setViewModePreferenceUseCase = mock<SetViewModePreferenceUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSortingPreferenceUseCase,
            setSortingPreferenceUseCase,
            monitorViewModePreferenceUseCase,
            setViewModePreferenceUseCase,
        )
    }

    private fun initTestClass(
        sortingPreference: SortingPreference = SortingPreference.PerFolder,
        viewModePreference: ViewModePreference = ViewModePreference.PerFolder,
    ) {
        monitorSortingPreferenceUseCase.stub {
            on { invoke() }.thenReturn(flowOf(sortingPreference))
        }
        monitorViewModePreferenceUseCase.stub {
            on { invoke() }.thenReturn(flowOf(viewModePreference))
        }
        underTest = SortingAndViewModeSettingsViewModel(
            monitorSortingPreferenceUseCase = monitorSortingPreferenceUseCase,
            setSortingPreferenceUseCase = setSortingPreferenceUseCase,
            monitorViewModePreferenceUseCase = monitorViewModePreferenceUseCase,
            setViewModePreferenceUseCase = setViewModePreferenceUseCase,
        )
    }

    @Test
    fun `test that uiState emits the monitored preferences`() = runTest {
        initTestClass(
            sortingPreference = SortingPreference.AllFolders,
            viewModePreference = ViewModePreference.PerFolder,
        )

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.sortingPreference).isEqualTo(SortingPreference.AllFolders)
            assertThat(state.viewModePreference).isEqualTo(ViewModePreference.PerFolder)
        }
    }

    @Test
    fun `test that setSortingPreference calls the use case`() = runTest {
        initTestClass()

        underTest.setSortingPreference(SortingPreference.AllFolders)

        verify(setSortingPreferenceUseCase).invoke(SortingPreference.AllFolders)
    }

    @Test
    fun `test that setViewModePreference calls the use case`() = runTest {
        initTestClass()

        underTest.setViewModePreference(ViewModePreference.AllFolders)

        verify(setViewModePreferenceUseCase).invoke(ViewModePreference.AllFolders)
    }

    private suspend fun ReceiveTurbine<SortingAndViewModeSettingsUiState>.awaitDataState(): SortingAndViewModeSettingsUiState.Data {
        var item = awaitItem()
        while (item !is SortingAndViewModeSettingsUiState.Data) {
            item = awaitItem()
        }
        return item
    }
}
