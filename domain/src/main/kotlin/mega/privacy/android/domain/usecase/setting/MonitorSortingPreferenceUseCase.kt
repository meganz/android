package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitor the sorting order preference, defaulting to [SortingPreference.PerFolder]
 */
class MonitorSortingPreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke(): Flow<SortingPreference> =
        settingsRepository.monitorSortingPreference().map { it ?: SortingPreference.PerFolder }
}
