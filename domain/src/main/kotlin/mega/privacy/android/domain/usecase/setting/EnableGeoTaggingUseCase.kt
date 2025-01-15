package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Enable Geo Tagging Use Case
 */
class EnableGeoTaggingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(value: Boolean) {
        settingsRepository.enableGeoTagging(value)
    }
}