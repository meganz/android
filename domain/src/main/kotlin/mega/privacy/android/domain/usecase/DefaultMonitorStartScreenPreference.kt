package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default monitor start screen preference
 *
 * @property settingsRepository
 */
class DefaultMonitorStartScreenPreference @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : MonitorStartScreenPreference {
    override fun invoke() = settingsRepository.monitorPreferredStartScreen()
        .mapNotNull { it ?: StartScreen.Home }
}