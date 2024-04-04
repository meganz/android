package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitor show hidden items
 *
 * @property settingsRepository
 */
class MonitorShowHiddenItemsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = settingsRepository.monitorShowHiddenItems()
}
