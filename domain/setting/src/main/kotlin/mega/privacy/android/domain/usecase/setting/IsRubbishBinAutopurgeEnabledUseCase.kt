package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Use case for checking if the rubbish bin autopurge is enabled.
 *
 */
class IsRubbishBinAutopurgeEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Checks if the rubbish bin autopurge is enabled.
     *
     * @return true if enabled, false otherwise.
     */
    suspend operator fun invoke(): Boolean = settingsRepository.isRubbishBinAutopurgeEnabled()
}