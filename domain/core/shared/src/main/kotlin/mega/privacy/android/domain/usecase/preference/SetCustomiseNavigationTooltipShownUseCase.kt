package mega.privacy.android.domain.usecase.preference

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to mark the customise navigation tooltip as shown
 */
class SetCustomiseNavigationTooltipShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        settingsRepository.setCustomiseNavigationTooltipShown()
    }
}
