package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to set download location path
 */
class SetDownloadLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Invoke
     * @param location as folder path [String]
     */
    suspend operator fun invoke(location: String) {
        settingsRepository.setDownloadLocation(location)
    }
}