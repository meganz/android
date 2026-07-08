package mega.privacy.android.domain.usecase.logout

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.usecase.folderpreference.ClearFolderPreferencesUseCase
import mega.privacy.android.domain.usecase.setting.SetSortingPreferenceUseCase
import mega.privacy.android.domain.usecase.setting.SetViewModePreferenceUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ClearSortingAndViewModePreferencesLogoutTaskTest {
    private lateinit var underTest: ClearSortingAndViewModePreferencesLogoutTask

    private val clearFolderPreferencesUseCase = mock<ClearFolderPreferencesUseCase>()
    private val setSortingPreferenceUseCase = mock<SetSortingPreferenceUseCase>()
    private val setViewModePreferenceUseCase = mock<SetViewModePreferenceUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = ClearSortingAndViewModePreferencesLogoutTask(
            clearFolderPreferencesUseCase = clearFolderPreferencesUseCase,
            setSortingPreferenceUseCase = setSortingPreferenceUseCase,
            setViewModePreferenceUseCase = setViewModePreferenceUseCase,
        )
    }

    @Test
    internal fun `test that per-folder preferences are cleared on logout success`() = runTest {
        underTest.onLogoutSuccess()

        verify(clearFolderPreferencesUseCase).invoke()
    }

    @Test
    internal fun `test that the sorting preference is reset to per folder on logout success`() =
        runTest {
            underTest.onLogoutSuccess()

            verify(setSortingPreferenceUseCase).invoke(SortingPreference.PerFolder)
        }

    @Test
    internal fun `test that the view mode preference is reset to per folder on logout success`() =
        runTest {
            underTest.onLogoutSuccess()

            verify(setViewModePreferenceUseCase).invoke(ViewModePreference.PerFolder)
        }
}
