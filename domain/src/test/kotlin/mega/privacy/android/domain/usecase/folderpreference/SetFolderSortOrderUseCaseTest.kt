package mega.privacy.android.domain.usecase.folderpreference

import com.google.common.truth.Truth.assertThat
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetFolderSortOrderUseCaseTest {
    private lateinit var underTest: SetFolderSortOrderUseCase

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorSortingPreferenceUseCase = mock<MonitorSortingPreferenceUseCase>()
    private val setFolderPreferenceUseCase = mock<SetFolderPreferenceUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetFolderSortOrderUseCase(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorSortingPreferenceUseCase = monitorSortingPreferenceUseCase,
            setFolderPreferenceUseCase = setFolderPreferenceUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValueUseCase,
            monitorSortingPreferenceUseCase,
            setFolderPreferenceUseCase,
        )
    }

    @Test
    fun `test that the full preference is stored when enabled and per folder`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(true)
        whenever(monitorSortingPreferenceUseCase())
            .thenReturn(flowOf(SortingPreference.PerFolder))
        var orElseCalled = false

        underTest(KEY, SortOrder.ORDER_SIZE_ASC, ViewType.GRID) { orElseCalled = true }

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
    fun `test that orElse is applied when per-folder sorting is off`() = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.SortingAndViewMode)).thenReturn(false)
        whenever(monitorSortingPreferenceUseCase())
            .thenReturn(flowOf(SortingPreference.PerFolder))
        var orElseArg: SortOrder? = null

        underTest(KEY, SortOrder.ORDER_SIZE_ASC, ViewType.GRID) { orElseArg = it }

        assertThat(orElseArg).isEqualTo(SortOrder.ORDER_SIZE_ASC)
        verify(setFolderPreferenceUseCase, never()).invoke(any())
    }

    companion object {
        private const val KEY = "AbCdEf123"
    }
}
