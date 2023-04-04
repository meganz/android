package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Pass through use case to set storage download location path
 */
class SetStorageDownloadLocationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * Invoke
     * @param location as folder path [String]
     */
    suspend operator fun invoke(location: String) {
        settingsRepository.setStorageDownloadLocation(location)
    }
}