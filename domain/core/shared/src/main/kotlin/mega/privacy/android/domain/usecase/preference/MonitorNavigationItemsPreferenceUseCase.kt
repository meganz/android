package mega.privacy.android.domain.usecase.preference

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to monitor the bottom navigation customisation preference
 */
class MonitorNavigationItemsPreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @return a flow of the current [NavigationItemsPreference], null if not set
     */
    operator fun invoke(): Flow<NavigationItemsPreference?> =
        settingsRepository.monitorNavigationItemsPreference()
}
