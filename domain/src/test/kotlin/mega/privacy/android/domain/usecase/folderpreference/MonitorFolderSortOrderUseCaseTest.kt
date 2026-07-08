package mega.privacy.android.domain.usecase.folderpreference

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSortingPreferenceUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorFolderSortOrderUseCaseTest {
    private lateinit var underTest: MonitorFolderSortOrderUseCase

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorSortingPreferenceUseCase = mock<MonitorSortingPreferenceUseCase>()
    private val monitorFolderPreferenceUseCase = mock<MonitorFolderPreferenceUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorFolderSortOrderUseCase(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorSortingPreferenceUseCase = monitorSortingPreferenceUseCase,
            monitorFolderPreferenceUseCase = monitorFolderPreferenceUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            monitorSortingPreferenceUseCase,
            monitorFolderPreferenceUseCase,
        )
    }

    @Test
    fun `test that the stored per-folder sort order is used when enabled and per folder`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
            whenever(monitorSortingPreferenceUseCase())
                .thenReturn(flowOf(SortingPreference.PerFolder))
            whenever(monitorFolderPreferenceUseCase(KEY))
                .thenReturn(flowOf(preference(SortOrder.ORDER_SIZE_ASC)))

            assertThat(underTest(KEY, orElse = flowOf(SortOrder.ORDER_MODIFICATION_ASC)).first())
                .isEqualTo(SortOrder.ORDER_SIZE_ASC)
        }

    @Test
    fun `test that the default ascending order is used when per folder and no value is stored`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
            whenever(monitorSortingPreferenceUseCase())
                .thenReturn(flowOf(SortingPreference.PerFolder))
            whenever(monitorFolderPreferenceUseCase(KEY)).thenReturn(flowOf(null))

            assertThat(underTest(KEY, orElse = flowOf(SortOrder.ORDER_SIZE_ASC)).first())
                .isEqualTo(SortOrder.ORDER_DEFAULT_ASC)
        }

    @Test
    fun `test that orElse is used when the preference is all folders`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
        whenever(monitorSortingPreferenceUseCase())
            .thenReturn(flowOf(SortingPreference.AllFolders))

        assertThat(underTest(KEY, orElse = flowOf(SortOrder.ORDER_SIZE_ASC)).first())
            .isEqualTo(SortOrder.ORDER_SIZE_ASC)
    }

    @Test
    fun `test that orElse is used when the flag is disabled`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(false)
        whenever(monitorSortingPreferenceUseCase())
            .thenReturn(flowOf(SortingPreference.PerFolder))

        assertThat(underTest(KEY, orElse = flowOf(SortOrder.ORDER_SIZE_ASC)).first())
            .isEqualTo(SortOrder.ORDER_SIZE_ASC)
    }

    private fun preference(sortOrder: SortOrder) = FolderPreference(
        folderKey = KEY,
        sortOrder = sortOrder,
        viewType = ViewType.LIST,
    )

    companion object {
        private const val KEY = "AbCdEf123"
    }
}
