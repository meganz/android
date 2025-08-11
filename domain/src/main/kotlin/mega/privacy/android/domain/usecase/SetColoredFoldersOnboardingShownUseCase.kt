package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to mark colored folders onboarding as shown
 */
class SetColoredFoldersOnboardingShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        settingsRepository.setColoredFoldersOnboardingShown(true)
    }
}
