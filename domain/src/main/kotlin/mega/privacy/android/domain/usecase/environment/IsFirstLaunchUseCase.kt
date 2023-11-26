package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Is first launch
 *
 * @property environmentRepository
 */
class IsFirstLaunchUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    /**
     * Invoke
     *
     * @return true if this is the first launch
     */
    suspend operator fun invoke(): Boolean = settingsRepository.getIsFirstLaunch() ?: true
}