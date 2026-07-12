package mega.privacy.android.domain.usecase.folderpreference

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
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
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorFolderViewTypeUseCaseTest {
    private lateinit var underTest: MonitorFolderViewTypeUseCase

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorViewModePreferenceUseCase = mock<MonitorViewModePreferenceUseCase>()
    private val monitorFolderPreferenceUseCase = mock<MonitorFolderPreferenceUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorFolderViewTypeUseCase(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorViewModePreferenceUseCase = monitorViewModePreferenceUseCase,
            monitorFolderPreferenceUseCase = monitorFolderPreferenceUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            monitorViewModePreferenceUseCase,
            monitorFolderPreferenceUseCase,
        )
    }

    @Test
    fun `test that the stored per-folder view type is used when enabled and per folder`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
            whenever(monitorViewModePreferenceUseCase())
                .thenReturn(flowOf(ViewModePreference.PerFolder))
            whenever(monitorFolderPreferenceUseCase(KEY)).thenReturn(flowOf(preference(ViewType.GRID)))

            assertThat(
                underTest(
                    KEY,
                    orElse = flowOf(ViewType.LIST)
                ).first()
            ).isEqualTo(ViewType.GRID)
        }

    @Test
    fun `test that the default list view is used when per folder and no value is stored`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
            whenever(monitorViewModePreferenceUseCase())
                .thenReturn(flowOf(ViewModePreference.PerFolder))
            whenever(monitorFolderPreferenceUseCase(KEY)).thenReturn(flowOf(null))

            assertThat(underTest(KEY, orElse = flowOf(ViewType.GRID)).first())
                .isEqualTo(ViewType.LIST)
        }

    @Test
    fun `test that orElse is used when the preference is all folders`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
        whenever(monitorViewModePreferenceUseCase())
            .thenReturn(flowOf(ViewModePreference.AllFolders))

        assertThat(underTest(KEY, orElse = flowOf(ViewType.GRID)).first()).isEqualTo(ViewType.GRID)
    }

    @Test
    fun `test that orElse is used when the flag is disabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(false)
        whenever(monitorViewModePreferenceUseCase())
            .thenReturn(flowOf(ViewModePreference.PerFolder))

        assertThat(underTest(KEY, orElse = flowOf(ViewType.GRID)).first()).isEqualTo(ViewType.GRID)
    }

    private fun preference(viewType: ViewType) = FolderPreference(
        folderKey = KEY,
        sortOrder = SortOrder.ORDER_DEFAULT_ASC,
        viewType = viewType,
    )

    companion object {
        private const val KEY = "AbCdEf123"
    }
}
