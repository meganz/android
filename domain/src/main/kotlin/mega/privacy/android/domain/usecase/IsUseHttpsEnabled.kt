package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Is use https preference enabled
 *
 */
class IsUseHttpsEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Invoke the use case
     *
     * @return the current value of the preference
     */
    suspend operator fun invoke(): Boolean = settingsRepository.isUseHttpsPreferenceEnabled()
}
