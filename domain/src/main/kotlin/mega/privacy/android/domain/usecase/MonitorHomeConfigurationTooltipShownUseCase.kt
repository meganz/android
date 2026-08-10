package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to check if the home configuration tooltip has been shown
 */
class MonitorHomeConfigurationTooltipShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @return true if the tooltip has been shown, false otherwise
     */
    operator fun invoke() = settingsRepository.monitorHomeConfigurationTooltipShown()
}
