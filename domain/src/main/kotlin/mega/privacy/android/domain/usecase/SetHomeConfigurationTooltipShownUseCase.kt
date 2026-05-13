package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to mark the home configuration tooltip as shown
 */
class SetHomeConfigurationTooltipShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        settingsRepository.setHomeConfigurationTooltipShown(true)
    }
}
