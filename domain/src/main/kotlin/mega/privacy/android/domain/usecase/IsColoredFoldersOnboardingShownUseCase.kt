package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.first
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to check if colored folders onboarding has been shown
 */
class IsColoredFoldersOnboardingShownUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     *
     * @return true if onboarding has been shown, false otherwise
     */
    suspend operator fun invoke(): Boolean =
        settingsRepository.monitorColoredFoldersOnboardingShown().first()
}
