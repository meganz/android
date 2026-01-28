package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case to set the show copyright flag directly
 */
class SetShowCopyrightUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     * @param show true to show copyright, false to hide it
     */
    suspend operator fun invoke(show: Boolean) {
        settingsRepository.setShowCopyright(show)
    }
}
