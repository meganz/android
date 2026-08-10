package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Set the view mode preference
 */
class SetViewModePreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(preference: ViewModePreference) =
        settingsRepository.setViewModePreference(preference)
}
