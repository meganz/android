package mega.privacy.android.domain.usecase.folderpreference

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorViewModePreferenceUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetFolderViewTypeUseCaseTest {
    private lateinit var underTest: SetFolderViewTypeUseCase

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorViewModePreferenceUseCase = mock<MonitorViewModePreferenceUseCase>()
    private val setFolderPreferenceUseCase = mock<SetFolderPreferenceUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetFolderViewTypeUseCase(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorViewModePreferenceUseCase = monitorViewModePreferenceUseCase,
            setFolderPreferenceUseCase = setFolderPreferenceUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            monitorViewModePreferenceUseCase,
            setFolderPreferenceUseCase,
        )
    }

    @Test
    fun `test that the full preference is stored when enabled and per folder`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
        whenever(monitorViewModePreferenceUseCase())
            .thenReturn(flowOf(ViewModePreference.PerFolder))
        var orElseCalled = false

        underTest(KEY, ViewType.GRID, SortOrder.ORDER_SIZE_ASC) { orElseCalled = true }

        verify(setFolderPreferenceUseCase).invoke(
            FolderPreference(
                folderKey = KEY,
                sortOrder = SortOrder.ORDER_SIZE_ASC,
                viewType = ViewType.GRID,
            )
        )
        assertThat(orElseCalled).isFalse()
    }

    @Test
    fun `test that orElse is applied when per-folder view mode is off`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
        whenever(monitorViewModePreferenceUseCase())
            .thenReturn(flowOf(ViewModePreference.AllFolders))
        var orElseArg: ViewType? = null

        underTest(KEY, ViewType.GRID, SortOrder.ORDER_SIZE_ASC) { orElseArg = it }

        assertThat(orElseArg).isEqualTo(ViewType.GRID)
        verify(setFolderPreferenceUseCase, never()).invoke(org.mockito.kotlin.any())
    }

    companion object {
        private const val KEY = "AbCdEf123"
    }
}
