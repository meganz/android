package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default monitor hide recent activity setting
 *
 * @property settingsRepository
 */
class MonitorHideRecentActivityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke() = settingsRepository.monitorHideRecentActivity().map { it ?: false }
}
