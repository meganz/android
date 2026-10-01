package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Set the sorting order preference
 */
class SetSortingPreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(preference: SortingPreference) =
        settingsRepository.setSortingPreference(preference)
}
