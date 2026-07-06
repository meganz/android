package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitor the view mode preference, defaulting to [ViewModePreference.PerFolder]
 */
class MonitorViewModePreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke(): Flow<ViewModePreference> =
        settingsRepository.monitorViewModePreference().map { it ?: ViewModePreference.PerFolder }
}
