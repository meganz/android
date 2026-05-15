package mega.privacy.android.domain.usecase.home

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Resets every persisted home widget configuration, so the next session starts from defaults.
 */
class ResetHomeWidgetConfigurationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        settingsRepository.resetHomeScreenWidgetConfigurations()
    }
}
