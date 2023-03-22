package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Is camera sync enabled
 *
 */
class IsCameraSyncEnabledUseCase @Inject constructor(private val settingsRepository: SettingsRepository) {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke() = settingsRepository.isCameraSyncPreferenceEnabled()
}