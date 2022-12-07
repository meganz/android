package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default monitor hide recent activity setting
 *
 * @property settingsRepository
 */
class DefaultMonitorHideRecentActivity @Inject constructor(private val settingsRepository: SettingsRepository) :
    MonitorHideRecentActivity {
    override fun invoke() = settingsRepository.monitorHideRecentActivity()
        .map { it ?: false }
}