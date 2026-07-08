package mega.privacy.android.domain.usecase.logout

import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.usecase.folderpreference.ClearFolderPreferencesUseCase
import mega.privacy.android.domain.usecase.setting.SetSortingPreferenceUseCase
import mega.privacy.android.domain.usecase.setting.SetViewModePreferenceUseCase
import javax.inject.Inject

/**
 * Clears the sorting and view mode preferences on logout so the next session starts clean:
 * the per-folder preferences are removed and the global scope preferences reset to their default.
 */
class ClearSortingAndViewModePreferencesLogoutTask @Inject constructor(
    private val clearFolderPreferencesUseCase: ClearFolderPreferencesUseCase,
    private val setSortingPreferenceUseCase: SetSortingPreferenceUseCase,
    private val setViewModePreferenceUseCase: SetViewModePreferenceUseCase,
) : LogoutTask {

    override suspend fun onLogoutSuccess() {
        clearFolderPreferencesUseCase()
        setSortingPreferenceUseCase(SortingPreference.PerFolder)
        setViewModePreferenceUseCase(ViewModePreference.PerFolder)
    }
}
