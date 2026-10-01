package mega.privacy.android.domain.usecase.preference

import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to set the bottom navigation customisation preference
 */
class SetNavigationItemsPreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @param preference the [NavigationItemsPreference] to persist
     */
    suspend operator fun invoke(preference: NavigationItemsPreference) {
        settingsRepository.setNavigationItemsPreference(preference)
    }
}
